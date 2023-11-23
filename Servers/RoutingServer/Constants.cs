namespace LetsGoBikingServer
{
    public static class Constants
    {
        public const string BaseAddress = "http://localhost:8000/LetsGoBikingServer/RoutingService";
        public const string EnvOrsStringApiKey = "ORS_API_KEY";
        public const string EnvOrsStringBackupApiKey = "ORS_API_BACKUP_KEYS";
        public const string OrsBaseAddressCycling = "https://api.openrouteservice.org/v2/directions/cycling-regular/";
        public const string OrsBaseAddressWalking = "https://api.openrouteservice.org/v2/directions/foot-walking/";
        public const string NominatimBaseAddressSearch = "https://nominatim.openstreetmap.org/search";
        public const string NominatimBaseAddressReverse = "https://nominatim.openstreetmap.org/reverse";
        public const string ActiveMQBrokerUri = "activemq:tcp://localhost:61616";
        public const string ActiveMQQueueName = "LetsGoBikingQueue";
    }
}