import { useEffect } from "react";
import { Client } from "@stomp/stompjs";

function websocketUrl(apiBaseUrl) {
  return apiBaseUrl.replace(/^http/, "ws").replace(/\/$/, "") + "/ws-notifications";
}

export function useCustomerNotifications({
  apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8082",
  customerId,
  onNotification,
  onConnectionError,
}) {
  useEffect(() => {
    if (!customerId) {
      return undefined;
    }

    const client = new Client({
      brokerURL: websocketUrl(apiBaseUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      connectionTimeout: 10000,
      onConnect: () => {
        client.subscribe(`/topic/notifications/${customerId}`, (message) => {
          try {
            onNotification?.(JSON.parse(message.body));
          } catch (error) {
            onConnectionError?.(error);
          }
        });
      },
      onStompError: (frame) => {
        onConnectionError?.(new Error(frame.headers.message ?? "Notification connection failed"));
      },
      onWebSocketError: (event) => {
        onConnectionError?.(event);
      },
    });

    client.activate();

    return () => {
      void client.deactivate();
    };
  }, [apiBaseUrl, customerId, onConnectionError, onNotification]);
}
