using System.ServiceModel;
using System.Threading.Tasks;

namespace ProxyServer
{
    [ServiceContract]
    public interface IJCDService
    {
        [OperationContract]
        Task<Station> GetClosestStationAsync(SimplifiedGeoCoordinate coordinates, string city, int minBikes);
    }
}