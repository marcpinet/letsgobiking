using System;
using System.Linq;
using System.Runtime.Caching;
using System.ServiceModel;
using System.Threading.Tasks;

namespace CachingServer
{
    [ServiceBehavior(InstanceContextMode = InstanceContextMode.Single, IncludeExceptionDetailInFaults = true)]
    public class CachingServer
    {
        private static readonly MemoryCache _cache = MemoryCache.Default;

        public async Task<T> GetOrSet<T>(string cacheKey, Func<Task<T>> retrieveFunction, TimeSpan duration)
            where T : class
        {
            if (!_cache.Contains(cacheKey))
            {
                Console.WriteLine("Caching Stations for key " + cacheKey + "...");
                var data = await retrieveFunction();
                if (data == null || data.Equals(default(T)))
                    Console.WriteLine("Nothing to cache for key " + cacheKey + "...");
                _cache.Add(cacheKey, data,
                    new CacheItemPolicy { AbsoluteExpiration = DateTimeOffset.Now.Add(duration) });
            }
            else
            {
                Console.WriteLine("Getting Stations from cache for key " + cacheKey + "...");
            }

            return _cache.Get(cacheKey) as T;
        }

        public void ClearCache()
        {
            var cacheItems = _cache.ToList();
            foreach (var item in cacheItems)
                _cache.Remove(item.Key);
        }
    }
}