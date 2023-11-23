using System;
using System.Text;
using Apache.NMS;
using Apache.NMS.ActiveMQ;

namespace LetsGoBikingServer
{
    public class ActiveMQProducer : IDisposable
    {
        private readonly IConnection connection;
        private readonly IConnectionFactory connectionFactory;
        private readonly IMessageProducer producer;
        private readonly string queueName;
        private readonly ISession session;

        public ActiveMQProducer(string brokerUri, string queueName)
        {
            this.queueName = queueName;
            connectionFactory = new ConnectionFactory(brokerUri);
            connection = connectionFactory.CreateConnection();
            connection.Start();

            session = connection.CreateSession(AcknowledgementMode.AutoAcknowledge);
            IDestination destination = session.GetQueue(queueName);
            producer = session.CreateProducer(destination);
        }

        public void Dispose()
        {
            producer?.Close();
            session?.Close();
            connection?.Close();
        }

        public void Send(string message)
        {
            message = Encoding.UTF8.GetString(Encoding.UTF8.GetBytes(message));
            var textMessage = session.CreateTextMessage(message);
            producer.Send(textMessage);
        }

        public override bool Equals(object obj)
        {
            return obj is ActiveMQProducer producer && queueName == producer.queueName;
        }

        // CompareTo method
        public int CompareTo(object obj)
        {
            if (obj == null) return 1;

            var otherProducer = obj as ActiveMQProducer;
            if (otherProducer != null)
                return string.Compare(queueName, otherProducer.queueName, StringComparison.Ordinal);
            throw new ArgumentException("Object is not a ActiveMQProducer");
        }
    }
}