using System.Globalization;
using System.Runtime.Serialization;
using Newtonsoft.Json;

namespace ProxyServer
{
    public class Station
    {
        public static string StatusOpen = "OPEN";
        public static string StatusClosed = "CLOSED";
        public int number { get; set; }
        public string contract_name { get; set; }
        public string name { get; set; }
        public string address { get; set; }
        public Position position { get; set; }
        public bool banking { get; set; }
        public bool bonus { get; set; }
        public int bike_stands { get; set; }
        public int available_bike_stands { get; set; }
        public int available_bikes { get; set; }
        public string status { get; set; }
        public long last_update { get; set; }

        public override string ToString()
        {
            return JsonConvert.SerializeObject(this, Formatting.Indented);
        }
    }

    public class Position
    {
        public double lat { get; set; }
        public double lng { get; set; }
    }

    [DataContract]
    public class SimplifiedGeoCoordinate
    {
        public SimplifiedGeoCoordinate(double latitude, double longitude)
        {
            Latitude = latitude;
            Longitude = longitude;
        }

        [DataMember(Name = "latitude")] public double Latitude { get; set; }

        [DataMember(Name = "longitude")] public double Longitude { get; set; }

        public override string ToString()
        {
            return
                $"{Longitude.ToString(CultureInfo.InvariantCulture)},{Latitude.ToString(CultureInfo.InvariantCulture)}";
        }
    }
}