using System.Runtime.Serialization;
using System.ServiceModel;

namespace LetsGoBikingServer
{
    [DataContract]
    public class ServiceException
    {
        public ServiceException(string message)
        {
            ErrorMessage = message;
        }

        [DataMember] public string ErrorMessage { get; set; }
    }

    public class OpenRouteServiceAPIException : FaultException<ServiceException>
    {
        public OpenRouteServiceAPIException(string message)
            : base(new ServiceException(message))
        {
        }
    }

    public class JCDServiceAPIException : FaultException<ServiceException>
    {
        public JCDServiceAPIException(string message)
            : base(new ServiceException(message))
        {
        }
    }

    public class NominatimAPIException : FaultException<ServiceException>
    {
        public NominatimAPIException(string message)
            : base(new ServiceException(message))
        {
        }
    }
}