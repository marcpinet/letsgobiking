using System;
using System.Collections.Generic;
using System.Globalization;
using System.Linq;
using System.Text;
using Newtonsoft.Json.Linq;

namespace LetsGoBikingServer
{
    public static class ParsingUtils
    {
        public static Itinerary ParseItinerary(JObject jsonData, bool cycling)
        {
            var itinerary = new Itinerary();
            itinerary.IsBicycle = cycling;
            itinerary.Segments = new List<Segment>();

            var segments = jsonData["features"][0]?["properties"]?["segments"];
            var coordinates = jsonData["features"]?[0]?["geometry"]?["coordinates"];
            var coordObject = coordinates?.ToObject<List<double[]>>().ToList();
            foreach (var seg in segments)
            {
                var segment = new Segment
                {
                    Distance = (double)seg["distance"],
                    Duration = (double)seg["duration"],
                    Steps = new List<Step>()
                };

                var steps = seg["steps"];
                foreach (var st in steps)
                {
                    var wayPoints = st["way_points"].ToObject<int[]>();
                    if (wayPoints != null)
                    {
                        var step = new Step
                        {
                            Distance = (double)st["distance"],
                            Duration = (double)st["duration"],
                            Instruction = (string)st["instruction"],
                            WayPoints = wayPoints,
                            Coordinates = coordObject.GetRange(wayPoints[0], wayPoints[1] - wayPoints[0] + 1)
                        };

                        segment.Steps.Add(step);
                    }
                }

                itinerary.Segments.Add(segment);
            }

            itinerary.Geometry = new Geometry();
            itinerary.Geometry.Coordinates = new List<double[]>();
            foreach (var coordinate in coordinates)
            {
                var coord = new double[2];
                coord[0] = (double)coordinate[1];
                coord[1] = (double)coordinate[0];
                itinerary.Geometry.Coordinates.Add(coord);
            }

            return itinerary;
        }

        public static string GenerateUniqueID()
        {
            var allowedCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            var random = new Random();
            var chars = new char[10];
            for (var i = 0; i < 10; i++)
                chars[i] = allowedCharacters[random.Next(0, allowedCharacters.Length)];
            return Constants.ActiveMQQueueName + "-" + new string(chars);
        }

        public static void UpdateItineraries(List<Itinerary> itineraries, int b)
        {
            if (itineraries == null || itineraries.Count == 0 || b <= 0) return;

            var stepsToRemove = b;
            var continueRemoving = true;

            for (var i = 0; i < itineraries.Count && continueRemoving; i++)
            {
                var itinerary = itineraries[i];
                for (var j = 0; j < itinerary.Segments.Count && continueRemoving; j++)
                {
                    var segment = itinerary.Segments[j];

                    if (segment.Steps.Count <= stepsToRemove)
                    {
                        stepsToRemove -= segment.Steps.Count;
                        itinerary.Segments.RemoveAt(j--);
                    }
                    else
                    {
                        segment.Steps.RemoveRange(0, stepsToRemove);
                        continueRemoving = false;
                    }
                }

                if (itinerary.Segments.Count == 0)
                    itineraries.RemoveAt(i--);
            }
        }

        public static string ConvertCoordinatesListToText(List<List<double[]>> coordinates)
        {
            var sb = new StringBuilder();
            sb.Append("[");

            const double TOLERANCE = 0.000001;

            for (var i = 0; i < coordinates.Count; i++)
            {
                if (i > 0)
                {
                    RemoveDuplicateCoordinates(coordinates[i - 1], coordinates[i], TOLERANCE);
                    sb.Append(",");
                }

                sb.Append("[");
                for (var j = 0; j < coordinates[i].Count; j++)
                {
                    if (j > 0)
                        sb.Append(",");

                    sb.AppendFormat("[{0},{1}]", coordinates[i][j][0].ToString(CultureInfo.InvariantCulture),
                        coordinates[i][j][1].ToString(CultureInfo.InvariantCulture));
                }

                sb.Append("]");
            }

            sb.Append("]");
            return sb.ToString();
        }

        public static void RemoveDuplicateCoordinates(List<double[]> firstCoordinates,
            List<double[]> secondCoordinates, double tolerance)
        {
            var firstCount = firstCoordinates.Count;
            var secondCount = secondCoordinates.Count;

            var firstStartIndex = firstCount > 5 ? firstCount - 5 : 0;
            var secondEndIndex = secondCount > 5 ? 5 : secondCount;

            var lastCoordsFirstItinerary = new List<double[]>();
            var firstCoordsSecondItinerary = new List<double[]>();

            for (var i = firstStartIndex; i < firstCount; i++) lastCoordsFirstItinerary.Add(firstCoordinates[i]);
            for (var i = 0; i < secondEndIndex; i++) firstCoordsSecondItinerary.Add(secondCoordinates[i]);

            foreach (var coord1 in lastCoordsFirstItinerary)
            foreach (var coord2 in firstCoordsSecondItinerary)
                if (Math.Abs(coord1[0] - coord2[0]) < tolerance && Math.Abs(coord1[1] - coord2[1]) < tolerance)
                {
                    firstCoordinates.Remove(coord1);
                    secondCoordinates.Remove(coord2);
                }
        }
    }
}