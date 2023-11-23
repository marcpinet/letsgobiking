using System.Collections.Generic;
using System.ServiceModel;
using System.Threading.Tasks;

namespace LetsGoBikingServer
{
    [ServiceContract]
    public interface IRoutingService
    {
        [OperationContract]
        Task<List<Itinerary>> GetItineraries(string origin, string destination);

        [OperationContract]
        Task<string> GetItineraryStepByStep(string origin, string destination, string uniqueId = null);

        [OperationContract]
        void GetItineraryUpdate(string uniqueId);
    }
}