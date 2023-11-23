using System;
using System.ServiceModel;
using System.ServiceModel.Description;

namespace CachingServer
{
    internal class Program
    {
        public static void Main(string[] args)
        {
            var baseAddress = new Uri(Constants.BaseAddress);

            using (var host = new ServiceHost(typeof(CachingServer), baseAddress))
            {
                var smb = new ServiceMetadataBehavior
                {
                    HttpGetEnabled = true,
                    MetadataExporter = { PolicyVersion = PolicyVersion.Policy15 }
                };


                host.Description.Behaviors.Add(smb);

                host.Open();

                Console.WriteLine("CachingServer is ready at {0}", Constants.BaseAddress);
                Console.WriteLine("Press <Enter> to stop the service.");
                Console.ReadLine();

                host.Close();
            }
        }
    }
}