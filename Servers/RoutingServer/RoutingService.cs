using System;
using System.Collections.Generic;
using System.Device.Location;
using System.Globalization;
using System.Linq;
using System.Net.Http;
using System.ServiceModel;
using System.Threading.Tasks;
using LetsGoBikingServer.JCDService;
using Newtonsoft.Json.Linq;

namespace LetsGoBikingServer
{
    [ServiceBehavior(InstanceContextMode = InstanceContextMode.Single, IncludeExceptionDetailInFaults = true)]
    public class RoutingService : IRoutingService
    {
        private const int MESSAGE_LIMIT = 25;
        private static int _BACKUP_API_KEY_INDEX;

        private static readonly Dictionary<string, Dictionary<ActiveMQProducer, KeyValuePair<List<Itinerary>, int>>>
            _itineraries =
                new Dictionary<string, Dictionary<ActiveMQProducer, KeyValuePair<List<Itinerary>, int>>>();


        private static readonly JCDServiceClient jcdServiceClient = new JCDServiceClient();
        private readonly HttpClient _client;
        private readonly NominatimUtils _nominatimUtils;

        public RoutingService()
        {
            _client = new HttpClient();
            _nominatimUtils = new NominatimUtils(_client);
        }

        public async Task<List<Itinerary>> GetItineraries(string origin, string destination, int minBikes = 1)
        {
            // Case where origin and/or destination is/are a geo coordinate like lat,lng and doesn't contain alphabet characters
            var isOriginCoordinates = origin.Contains(",") && !origin.Any(char.IsLetter);
            var isDestinationCoordinates = destination.Contains(",") && !destination.Any(char.IsLetter);

            GeoCoordinate originCoordinates;
            GeoCoordinate destinationCoordinates;

            if (!isOriginCoordinates)
                originCoordinates = await _nominatimUtils.GetGeoCodeAsync(origin);
            else
                originCoordinates = new GeoCoordinate(
                    double.Parse(origin.Split(',')[0], CultureInfo.InvariantCulture),
                    double.Parse(origin.Split(',')[1], CultureInfo.InvariantCulture)
                );

            if (!isDestinationCoordinates)
                destinationCoordinates = await _nominatimUtils.GetGeoCodeAsync(destination);
            else
                destinationCoordinates = new GeoCoordinate(
                    double.Parse(destination.Split(',')[0], CultureInfo.InvariantCulture),
                    double.Parse(destination.Split(',')[1], CultureInfo.InvariantCulture)
                );

            var originCoordinatesGeoSimplified = new SimplifiedGeoCoordinate();
            originCoordinatesGeoSimplified.latitude = originCoordinates.Latitude;
            originCoordinatesGeoSimplified.longitude = originCoordinates.Longitude;
            var destinationCoordinatesGeoSimplfied = new SimplifiedGeoCoordinate();
            destinationCoordinatesGeoSimplfied.latitude = destinationCoordinates.Latitude;
            destinationCoordinatesGeoSimplfied.longitude = destinationCoordinates.Longitude;

            var originCity = await _nominatimUtils.GetCityFromCoordinatesAsync(originCoordinates);
            var destinationCity = await _nominatimUtils.GetCityFromCoordinatesAsync(destinationCoordinates);
            var originStation =
                await jcdServiceClient.GetClosestStationAsync(originCoordinatesGeoSimplified, originCity, minBikes);
            var destinationStation =
                await jcdServiceClient.GetClosestStationAsync(destinationCoordinatesGeoSimplfied, destinationCity,
                    minBikes);
            var originStationCoordinates = new GeoCoordinate(originStation.position.lat, originStation.position.lng);
            var destinationStationCoordinates =
                new GeoCoordinate(destinationStation.position.lat, destinationStation.position.lng);

            var itinerary1 = await GetItineraryFromCoordinates(originCoordinates, originStationCoordinates, false);
            var itinerary2 = await GetItineraryFromCoordinates(originStationCoordinates, destinationStationCoordinates);
            var itinerary3 =
                await GetItineraryFromCoordinates(destinationStationCoordinates, destinationCoordinates, false);

            itinerary1.Concatenate(itinerary2);
            itinerary2.Concatenate(itinerary3);

            var directItinerary = await GetItineraryFromCoordinates(originCoordinates, destinationCoordinates, false);

            if (directItinerary < itinerary1)
                return new List<Itinerary> { directItinerary };

            return new List<Itinerary> { itinerary1, itinerary2, itinerary3 };
        }

        public async Task<string> GetItineraryStepByStep(string origin, string destination, int minBikes,
            string uniqueId = null)
        {
            var itineraries = await GetItineraries(origin, destination, minBikes);

            ActiveMQProducer producer;

            if (uniqueId == null)
            {
                uniqueId = ParsingUtils.GenerateUniqueID();
                producer = new ActiveMQProducer(Constants.ActiveMQBrokerUri, uniqueId);
            }
            else
            {
                producer = _itineraries[uniqueId].Keys.ToList()[0];
            }

            try
            {
                var itinerary = itineraries[0];
                var segments = itinerary.Segments;
                var steps = segments[0].Steps;

                var coordinates = new List<List<double[]>>();
                foreach (var it in itineraries)
                    coordinates.Add(it.extractCoordinatesFromEachStep());

                var messageCount = Math.Min(MESSAGE_LIMIT, steps.Count);
                for (var i = 0; i < messageCount; i++)
                    if (i < messageCount - 1)
                        producer.Send(steps[i] + "|" + ParsingUtils.ConvertCoordinatesListToText(coordinates));
                    else
                        producer.Send(steps[i] + "|" + ParsingUtils.ConvertCoordinatesListToText(coordinates) + "|" +
                                      "END");

                Console.WriteLine("Sent message(s)!");
                ParsingUtils.UpdateItineraries(itineraries, MESSAGE_LIMIT);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                producer.Send("FINISHED");
            }

            if (!_itineraries.ContainsKey(uniqueId))
            {
                _itineraries.Add(uniqueId, new Dictionary<ActiveMQProducer, KeyValuePair<List<Itinerary>, int>>());
                _itineraries[uniqueId].Add(producer, new KeyValuePair<List<Itinerary>, int>(itineraries, minBikes));
            }
            else
            {
                _itineraries[uniqueId][producer] = new KeyValuePair<List<Itinerary>, int>(itineraries, minBikes);
            }

            return uniqueId; // Queue name
        }

        public async void GetItineraryUpdate(string uniqueId)
        {
            if (!_itineraries.TryGetValue(uniqueId, out var itineraryPair) ||
                itineraryPair.Values.FirstOrDefault().Key.Count == 0)
            {
                itineraryPair.Keys.FirstOrDefault()?.Send("FINISHED");
                _itineraries.Remove(uniqueId);
                return;
            }

            var currentItineraries = itineraryPair.Values.First();
            var producer = itineraryPair.Keys.First();

            var newItineraries = new List<Itinerary>();
            foreach (var itinerary in currentItineraries.Key)
            {
                var currentOrigin = itinerary.Segments.First().Steps.First().Coordinates.First();
                var currentDestination = itinerary.Segments.Last().Steps.Last().Coordinates.Last();

                newItineraries.Add(await GetItineraryFromCoordinates(
                    new GeoCoordinate(currentOrigin[1], currentOrigin[0]),
                    new GeoCoordinate(currentDestination[1], currentDestination[0]),
                    itinerary.IsBicycle));
            }

            try
            {
                var segments = newItineraries.First().Segments;
                var steps = segments.First().Steps;

                var messageCount = Math.Min(MESSAGE_LIMIT, steps.Count);
                var coordinates = new List<List<double[]>>();
                foreach (var it in newItineraries)
                    coordinates.Add(it.extractCoordinatesFromEachStep());

                for (var i = 0; i < messageCount; i++)
                    if (i < messageCount - 1)
                        producer.Send(steps[i] + "|" + ParsingUtils.ConvertCoordinatesListToText(coordinates));
                    else
                        producer.Send(steps[i] + "|" + ParsingUtils.ConvertCoordinatesListToText(coordinates) + "|" +
                                      "END");

                Console.WriteLine("Sent message(s)!");

                ParsingUtils.UpdateItineraries(newItineraries, MESSAGE_LIMIT);
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                producer.Send("FINISHED");
            }

            _itineraries[uniqueId] = new Dictionary<ActiveMQProducer, KeyValuePair<List<Itinerary>, int>>();
            _itineraries[uniqueId].Add(producer, new KeyValuePair<List<Itinerary>, int>(newItineraries,
                currentItineraries.Value));
        }

        private async Task<Itinerary> GetItineraryFromCoordinates(GeoCoordinate origin, GeoCoordinate destination,
            bool cycling = true)
        {
            var apiKey = Environment.GetEnvironmentVariable(Constants.EnvOrsStringApiKey);
            fun:
            var requestUri = BuildRequestUri(apiKey, origin, destination, cycling);

            Console.WriteLine("Service called: " + requestUri);

            var response = await _client.GetAsync(requestUri);
            if (!response.IsSuccessStatusCode)
                try
                {
                    var errorResponse = await response.Content.ReadAsStringAsync();
                    var errorMessage = JObject.Parse(errorResponse)["error"]?["message"]?.ToString();
                    throw new OpenRouteServiceAPIException(
                        $"Response is not a success for OpenRouteService: {errorMessage}");
                }
                catch (InvalidOperationException e)
                {
                    var apiKeys = Environment.GetEnvironmentVariable(Constants.EnvOrsStringBackupApiKey)
                        ?.Split(separator: ',');

                    if (_BACKUP_API_KEY_INDEX >= apiKeys.Length)
                        _BACKUP_API_KEY_INDEX = 0;

                    apiKey = apiKeys[_BACKUP_API_KEY_INDEX]; // In case I'm being rate limited during my presentation
                    Console.WriteLine("Used the backup API key n°" + _BACKUP_API_KEY_INDEX);
                    _BACKUP_API_KEY_INDEX++;
                    goto fun;
                }

            var jsonResponse = await response.Content.ReadAsStringAsync();
            return ParsingUtils.ParseItinerary(JObject.Parse(jsonResponse), cycling);
        }

        private string BuildRequestUri(string apiKey, GeoCoordinate originCoordinates,
            GeoCoordinate destinationCoordinates, bool cycling = true)
        {
            var origin =
                $"{originCoordinates.Longitude.ToString(CultureInfo.InvariantCulture)},{originCoordinates.Latitude.ToString(CultureInfo.InvariantCulture)}";
            var destination =
                $"{destinationCoordinates.Longitude.ToString(CultureInfo.InvariantCulture)},{destinationCoordinates.Latitude.ToString(CultureInfo.InvariantCulture)}";

            if (cycling)
                return $"{Constants.OrsBaseAddressCycling}?api_key={apiKey}&start={origin}&end={destination}";
            return $"{Constants.OrsBaseAddressWalking}?api_key={apiKey}&start={origin}&end={destination}";
        }
    }
}