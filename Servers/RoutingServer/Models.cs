using System;
using System.Collections.Generic;
using System.Runtime.Serialization;

namespace LetsGoBikingServer
{
    [DataContract]
    public class Itinerary
    {
        [DataMember(Name = "segments")] public List<Segment> Segments { get; set; }
        [DataMember(Name = "is_bicycle")] public bool IsBicycle { get; set; }
        [DataMember(Name = "geometry")] public Geometry Geometry { get; set; }

        public override string ToString()
        {
            var result = "Itinerary: \n";
            foreach (var segment in Segments)
                result += segment + "\n";
            result += $"IsBicycle = {IsBicycle}\n";
            return result;
        }

        public List<double[]> extractCoordinatesFromEachStep()
        {
            var coordinates = new List<double[]>();
            foreach (var segment in Segments)
            foreach (var step in segment.Steps)
            foreach (var coordinate in step.Coordinates)
                coordinates.Add(coordinate);
            return coordinates;
        }

        public void Concatenate(Itinerary other)
        {
            RemoveDuplicateCoordinates(Geometry.Coordinates, other.Geometry.Coordinates);
        }

        private void RemoveDuplicateCoordinates(List<double[]> firstCoordinates, List<double[]> secondCoordinates)
        {
            var firstCount = firstCoordinates.Count;
            var secondCount = secondCoordinates.Count;

            var firstStartIndex = firstCount > 5 ? firstCount - 5 : 0;
            var secondEndIndex = secondCount > 5 ? 5 : secondCount;

            var lastCoordsFirstItinerary = new List<double[]>();
            var firstCoordsSecondItinerary = new List<double[]>();

            for (var i = firstStartIndex; i < firstCount; i++) lastCoordsFirstItinerary.Add(firstCoordinates[i]);

            for (var i = 0; i < secondEndIndex; i++) firstCoordsSecondItinerary.Add(secondCoordinates[i]);

            var TOLERANCE = 0.000001;
            foreach (var coord1 in lastCoordsFirstItinerary)
            foreach (var coord2 in firstCoordsSecondItinerary)
                if (Math.Abs(coord1[0] - coord2[0]) < TOLERANCE && Math.Abs(coord1[1] - coord2[1]) < TOLERANCE)
                {
                    firstCoordinates.Remove(coord1);
                    secondCoordinates.Remove(coord2);
                }
        }

        public static bool operator >(Itinerary itinerary1, Itinerary itinerary2)
        {
            double duration1 = 0;
            double duration2 = 0;
            foreach (var segment in itinerary1.Segments)
                duration1 += segment.Duration;
            foreach (var segment in itinerary2.Segments)
                duration2 += segment.Duration;
            return duration1 > duration2;
        }

        public static bool operator <(Itinerary itinerary1, Itinerary itinerary2)
        {
            double duration1 = 0;
            double duration2 = 0;
            foreach (var segment in itinerary1.Segments)
                duration1 += segment.Duration;
            foreach (var segment in itinerary2.Segments)
                duration2 += segment.Duration;
            return duration1 < duration2;
        }
    }

    [DataContract]
    public class Geometry
    {
        [DataMember(Name = "coordinates")]
        public List<double[]> Coordinates { get; set; } // Each coordinate is a pair of doubles (latitude, longitude)

        public override string ToString()
        {
            var result = "Geometry: \n";
            foreach (var coordinate in Coordinates)
                result += $"[{coordinate[0]}, {coordinate[1]}], ";
            return result.TrimEnd(',', ' ') + "\n";
        }
    }

    [DataContract]
    public class Segment
    {
        [DataMember(Name = "steps")] public List<Step> Steps { get; set; }

        [DataMember(Name = "distance")] public double Distance { get; set; }

        [DataMember(Name = "duration")] public double Duration { get; set; }

        public override string ToString()
        {
            var result = $"Segment: Distance = {Distance}m, Duration = {Duration}s\n";
            foreach (var step in Steps)
                result += "  " + step + "\n";
            return result;
        }
    }

    [DataContract]
    public class Step
    {
        [DataMember(Name = "distance")] public double Distance { get; set; }

        [DataMember(Name = "duration")] public double Duration { get; set; }

        [DataMember(Name = "instruction")] public string Instruction { get; set; }

        [DataMember(Name = "coordinates")] public List<double[]> Coordinates { get; set; }

        [DataMember(Name = "way_points")] public int[] WayPoints { get; set; }

        public override string ToString()
        {
            var readableDistance = GetReadableDistance();
            var readableDuration = GetReadableDuration();

            return $"{Instruction} (~{readableDistance}, ~{readableDuration})";
        }

        private string GetReadableDistance()
        {
            if (Distance >= 1000)
                return $"{Math.Round(Distance / 1000.0)} km";

            return $"{Math.Round(Distance)} m";
        }

        private string GetReadableDuration()
        {
            if (Duration >= 3600)
                return $"{Math.Round(Duration / 3600.0)} hours";

            if (Duration >= 60)
                return $"{Math.Round(Duration / 60.0)} minutes";

            return $"{Math.Round(Duration)} seconds";
        }
    }
}