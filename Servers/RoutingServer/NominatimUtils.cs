using System;
using System.Device.Location;
using System.Globalization;
using System.Linq;
using System.Net.Http;
using System.Threading.Tasks;
using System.Web;
using Newtonsoft.Json.Linq;

namespace LetsGoBikingServer
{
    public class NominatimUtils
    {
        private readonly HttpClient _client;

        public NominatimUtils(HttpClient client)
        {
            _client = client;
            if (!_client.DefaultRequestHeaders.Contains("User-Agent"))
                _client.DefaultRequestHeaders.Add("User-Agent", "LetsGoBikingServer");
        }

        public async Task<string> GetCityFromCoordinatesAsync(GeoCoordinate coordinates)
        {
            var requestUri = BuildReverseGeocodeRequestUri(coordinates);
            var response = await ExecuteRequestAsync(requestUri);

            if (response == null)
                throw new NominatimAPIException("No place found for these coordinates: " + coordinates);

            var address = response["address"];

            var city = (string)address?["city"] ?? (string)address?["town"] ?? (string)address?["village"];
            if (city == null)
                throw new NominatimAPIException(
                    "It seems like you entered a whole country. Please enter a place or city.");
            return city;
        }

        public async Task<string> GetAddressFromCoordinatesAsync(GeoCoordinate coordinates)
        {
            var requestUri = BuildReverseGeocodeRequestUri(coordinates);
            var response = await ExecuteRequestAsync(requestUri);

            if (response == null)
                throw new NominatimAPIException("No place found for these coordinates: " + coordinates);

            var address = response["address"];

            var road = (string)address?["road"];
            var suburb = (string)address?["suburb"];
            var city = (string)address?["city"] ?? (string)address?["town"] ?? (string)address?["village"];
            var state = (string)address?["state"];
            var postcode = (string)address?["postcode"];
            var country = (string)address?["country"];

            var fullAddress = $"{road}, {suburb}, {city}, {state}, {postcode}, {country}";
            return fullAddress.Trim(',', ' ');
        }

        public async Task<GeoCoordinate> GetGeoCodeAsync(string place)
        {
            place = HttpUtility.UrlEncode(place);

            Console.WriteLine(place);

            var requestUri = BuildGeocodeRequestUri(place);
            var response = await ExecuteRequestAsync(requestUri);

            if (response == null)
                throw new NominatimAPIException("No place found for this address: " + place);

            return ParseGeoCode(response);
        }

        private string BuildGeocodeRequestUri(string coordinates)
        {
            return $"{Constants.NominatimBaseAddressSearch}?q={coordinates}&format=json&limit=1";
        }

        private string BuildReverseGeocodeRequestUri(GeoCoordinate coordinates)
        {
            var lon = coordinates.Longitude.ToString(CultureInfo.InvariantCulture);
            var lat = coordinates.Latitude.ToString(CultureInfo.InvariantCulture);

            return $"{Constants.NominatimBaseAddressReverse}?lat={lat}&lon={lon}&format=json&limit=1";
        }

        private GeoCoordinate ParseGeoCode(JToken jsonData)
        {
            var geoCoordinate = new GeoCoordinate((double)jsonData["lat"], (double)jsonData["lon"]);
            return geoCoordinate;
        }

        private async Task<JToken> ExecuteRequestAsync(string requestUri)
        {
            Console.WriteLine("Service called: " + requestUri);

            try
            {
                var response = await _client.GetAsync(requestUri);
                if (!response.IsSuccessStatusCode)
                    return null;

                var jsonResponse = await response.Content.ReadAsStringAsync();

                var parsedJson = JToken.Parse(jsonResponse);

                if (parsedJson is JArray)
                    return parsedJson.FirstOrDefault();
                if (parsedJson is JObject)
                    return parsedJson;
                return null;
            }
            catch (HttpRequestException ex)
            {
                throw new OpenRouteServiceAPIException($"Error fetching data: {ex.Message}" + " -> " + ex);
            }
        }
    }
}