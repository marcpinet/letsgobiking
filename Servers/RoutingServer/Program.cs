using System;
using System.ServiceModel;
using System.ServiceModel.Description;
using dotenv.net;

namespace LetsGoBikingServer
{
    internal static class Program
    {
        private static void Main(string[] args)
        {
            DotEnv.Load();
            using (var host = new ServiceHost(typeof(RoutingService), new Uri(Constants.BaseAddress)))
            {
                try
                {
                    var smb = new ServiceMetadataBehavior
                    {
                        HttpGetEnabled = true,
                        MetadataExporter = { PolicyVersion = PolicyVersion.Policy15 }
                    };
                    host.Description.Behaviors.Add(smb);

                    host.Open();

                    Console.WriteLine("RoutingService is ready at {0}", Constants.BaseAddress);
                    Console.WriteLine("Press <Enter> to stop the service.");
                    Console.ReadLine();

                    host.Close();
                }
                catch (CommunicationException ce)
                {
                    Console.WriteLine("An exception occurred: {0}", ce.Message);
                    host.Abort();
                }
            }
        }
    }
}