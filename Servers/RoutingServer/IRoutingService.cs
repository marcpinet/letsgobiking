using System.Collections.Generic;
using System.ServiceModel;
using System.Threading.Tasks;

namespace LetsGoBikingServer
{
    [ServiceContract]
    public interface IRoutingService
    {
        [OperationContract]
        Task<List<Itinerary>>
            GetItineraries(string origin, string destination, int minBikes); // not needed if we use ActiveMQ
        // Warning: removing this method will break the client (Itinerary class won't be generated in the client)

        [OperationContract]
        Task<string> GetItineraryStepByStep(string origin, string destination, int minBikes, string uniqueId = null);

        [OperationContract]
        void GetItineraryUpdate(string uniqueId);
    }
}