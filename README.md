# LetsGoBiking

## 📝 Description

This project involves the development of a self-hosted SOAP server (with cache, queue and proxy) in C# and a heavy client in Java for a routing service.

The server interfaces with the JC Decaux API and an external REST API to provide itinerary planning services, while the client application allows users to request and display these itineraries.

The project aims to demonstrate the practical application of SOAP and REST APIs, as well as the integration of various software components into a cohesive system.

This project was developed as part of an advanced IT course, requiring careful planning, understanding of web services, and efficient coding practices.

## 📦 Features

- Enter any place you want from origin to destination 🌎
- Map itinerary visualization 🗺️
- Real time data with live map update 🕒
- Fully GUI-zed 🪟
- Travel is organized from your current location to the nearest station, then from the station closest to your destination to your final destination 📍
- Self-hosted SOAP server in C# for itinerary planning 🌐
- Java-based heavy client for user interaction and data display 🖥️
- Integration with JCDecaux API and external REST APIs 🚲
- Advanced features like caching and message queuing with ActiveMQ 🚀
- Detailed documentation for installation and usage 📚

## ⚙️ Installation

### Requirements

- Java 11+
- Maven
- .NET Framework 4.8
- ActiveMQ

### Installation and step-by-step setup

> [!WARNING]  
> Because of Windows 10/11 port access and protected system resources policies, you need to run the servers as administrator to allow them to host the services on `localhost`. If you don't intend to run the servers elsewhere than from your IDE, make sure to run your IDE as administrator aswell.

> [!NOTE]  
> Assuming you've all your path environment variables ([msbuild](https://visualstudio.microsoft.com/downloads/?cid=learn-onpage-download-cta#build-tools-for-visual-studio-2022), [nuget](https://www.nuget.org/downloads), [activemq](https://activemq.apache.org/components/classic/download/) and [mvn](https://maven.apache.org/download.cgi)), you can directly run the `auto_start.bat`. Please, don't forget to setup the `.env file` in `/Server/.env`.

1. **Clone** the repository to your local machine.

```bash
git clone https://github.com/marcpinet/letsgobiking.git
```

2. **Create** a `.env` file in `Servers/`, using [.env.example](Servers/.env.example) and fill in the API Keys of [OpenRouteService](https://api.openrouteservice.org/) and [JCDecaux](https://developer.jcdecaux.com/#/home).

3. **Open** an ActiveMQ instance in a terminal. Make sure [you have it installed](https://activemq.apache.org/components/classic/download/) and added to environment variables.

```bash
activemq start
```

4. **Install, Build & Run (as administrator)** the servers using either command line or your preferred IDE (just open the `.sln`).

```bash
cd Servers
nuget restore LetsGoBikingServer.sln
msbuild /p:Configuration=Release /p:TargetFrameworkVersion=v4.8
start "Caching Server" .\CachingServer\bin\Release\CachingServer.exe
start "Proxy Server" .\ProxyServer\bin\Release\ProxyServer.exe
start "Routing Server" .\RoutingServer\bin\Release\RoutingServer.exe
```

5. **Install, Build & Run** the client using either command line or your preferred IDE.

```bash
cd ../Client
mvn clean install
mvn compile
mvn exec:java -Dexec.mainClass="com.polytech.mwsoc.Main"
```

## 💡 How to use

Once you did all the steps above, you'll be prompted to choose a starting place and a destination.

> [!NOTE]  
> The returned itinerary will **ALWAYS** be the shortest path. If, by walking, you're making it faster rather than by going to a bike station, it will return the walk itinerary instead.

### Examples

🔵 = JCDecaux bike itinerary
🔴 = Walking itinerary

#### Real Time with live update of the itinerary (with activemq) and steps

https://github.com/marcpinet/letsgobiking/assets/52708150/cd41313b-cd64-4a77-88ab-f765ce49eda7

#### Some map showcases and search addresses (note that you can type whatever you want, not only plain cities)

```
Enter an origin:
campus sophiatech

Enter a destination:
aubervilliers
```

https://github.com/marcpinet/letsgobiking/assets/52708150/75312282-bbd8-421e-b082-ad94fb81ea75

---

```
Enter an origin:
montpellier

Enter a destination:
madrid
```

https://github.com/marcpinet/letsgobiking/assets/52708150/b28420a7-046b-4f4a-ab7c-2327490d93c8

---

```
Enter an origin:
campus sophiatech

Enter a destination:
mouratoglou
```

![walk_only](https://github.com/marcpinet/letsgobiking/assets/52708150/69886f65-56ab-40b9-9ead-4dfeb78152e4)

## 🎯 TO-DO

- Allow the user to directly select the origin and destination on the map using waypoints
- Allow the user to select the minimum bike count inside a station to be counted
- Allow the user to select how many steps they want from the server between each update
- Reduce Nominatim calls to speed up the computation and add caching to their response

## ✍️ Authors

- Marc Pinet - *Initial work* - [marcpinet](https://github.com/marcpinet)
