using System;
using System.ServiceModel;
using System.ServiceModel.Description;
using dotenv.net;

namespace ProxyServer
{
    internal class Program
    {
        public static void Main(string[] args)
        {
            DotEnv.Load();

            var baseAddress = new Uri(Constants.BaseAddress);

            using (var host = new ServiceHost(typeof(JCDService), baseAddress))
            {
                var smb = new ServiceMetadataBehavior
                {
                    HttpGetEnabled = true,
                    MetadataExporter = { PolicyVersion = PolicyVersion.Policy15 }
                };


                host.Description.Behaviors.Add(smb);

                host.Open();

                Console.WriteLine("ProxyServer is ready at {0}", Constants.BaseAddress);
                Console.WriteLine("Press <Enter> to stop the service.");
                Console.ReadLine();

                host.Close();
            }
        }
    }
}