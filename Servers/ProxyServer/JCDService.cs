using System;
using System.Collections.Generic;
using System.Device.Location;
using System.Globalization;
using System.Net.Http;
using System.ServiceModel;
using System.Text;
using System.Threading.Tasks;
using Newtonsoft.Json;

namespace ProxyServer
{
    [ServiceBehavior(InstanceContextMode = InstanceContextMode.Single, IncludeExceptionDetailInFaults = true)]
    public class JCDService : IJCDService
    {
        private readonly CachingServer.CachingServer _cachingServer = new CachingServer.CachingServer();
        private readonly HttpClient _client = new HttpClient();

        public async Task<Station> GetClosestStationAsync(SimplifiedGeoCoordinate coordinates, string city,
            int minBikes)
        {
            var geoCoordinates = new GeoCoordinate(coordinates.Latitude, coordinates.Longitude);

            city = RemoveDiacritics(city);
            var stations = await GetAvailableStationsAsync(city, minBikes);

            if (stations == null || stations.Count == 0) stations = await ForceGetAvailableStationsAsync(minBikes);


            var closestStation = stations[0];
            var closestDistance =
                geoCoordinates.GetDistanceTo(
                    new GeoCoordinate(closestStation.position.lat, closestStation.position.lng));

            foreach (var station in stations)
            {
                var distance =
                    geoCoordinates.GetDistanceTo(new GeoCoordinate(station.position.lat, station.position.lng));
                if (distance < closestDistance)
                {
                    closestDistance = distance;
                    closestStation = station;
                }
            }

            return closestStation;
        }

        private async Task<List<Station>> ForceGetAvailableStationsAsync(int minBikes)
        {
            var stations = await GetStationsAsync();
            var availableStations = new List<Station>();

            foreach (var station in stations)
                if (station.status == Station.StatusOpen && station.available_bikes >= minBikes)
                    availableStations.Add(station);

            return availableStations;
        }

        private async Task<List<Station>> GetAvailableStationsAsync(string city, int minBikes)
        {
            var stations = await GetStationsAsync(city);
            var availableStations = new List<Station>();

            if (stations == null)
                return null;

            foreach (var station in stations)
                if (station.status == Station.StatusOpen && station.available_bikes >= minBikes)
                    availableStations.Add(station);
            return availableStations;
        }

        private async Task<List<Station>> GetStationsAsync(string city)
        {
            var cacheKey = $"Stations_{city}";
            var apiKey = Environment.GetEnvironmentVariable(Constants.EnvJcdecauxApiKey);

            try
            {
                var cachedResponse = await _cachingServer.GetOrSet(cacheKey,
                    async () =>
                    {
                        var response =
                            await _client.GetStringAsync($"{Constants.JcdBaseAdress}?apiKey={apiKey}&contract={city}");
                        return response;
                    }, TimeSpan.FromMinutes(5));
                return JsonConvert.DeserializeObject<List<Station>>(cachedResponse.Replace("null", "0"));
            }
            catch (Exception e)
            {
                return null;
            }
        }

        private async Task<List<Station>> GetStationsAsync()
        {
            var cacheKey = "Stations_All";
            var apiKey = Environment.GetEnvironmentVariable(Constants.EnvJcdecauxApiKey);

            var cachedResponse = await _cachingServer.GetOrSet(cacheKey,
                async () =>
                {
                    var response = await _client.GetStringAsync($"{Constants.JcdBaseAdress}?apiKey={apiKey}");
                    return response;
                }, TimeSpan.FromMinutes(5));

            return JsonConvert.DeserializeObject<List<Station>>(cachedResponse.Replace("null", "0"));
        }

        private string RemoveDiacritics(string text)
        {
            text = text.Replace(" ", "-");
            var normalizedString = text.Normalize(NormalizationForm.FormD);
            var stringBuilder = new StringBuilder();

            foreach (var c in normalizedString)
            {
                var unicodeCategory = CharUnicodeInfo.GetUnicodeCategory(c);
                if (unicodeCategory != UnicodeCategory.NonSpacingMark)
                    stringBuilder.Append(c);
            }

            return stringBuilder.ToString().Normalize(NormalizationForm.FormC);
        }
    }
}