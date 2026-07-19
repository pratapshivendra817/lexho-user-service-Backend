# Customer Notification WebSocket

The backend endpoint `/ws-notifications` uses STOMP. Do not connect with the browser
`WebSocket` class alone because it opens the socket without sending a STOMP
`CONNECT` frame.

Install the STOMP client:

```bash
npm install @stomp/stompjs
```

Use the hook after customer authentication:

```jsx
import { useCallback } from "react";
import { useCustomerNotifications } from "./useCustomerNotifications";

export default function CustomerApp({ customerId }) {
  const handleNotification = useCallback((notification) => {
    console.log("Customer notification", notification);
  }, []);

  const handleConnectionError = useCallback((error) => {
    console.error("Notification connection error", error);
  }, []);

  useCustomerNotifications({
    customerId,
    onNotification: handleNotification,
    onConnectionError: handleConnectionError,
  });

  return <div>Customer app</div>;
}
```

The hook:

- Connects to `ws://localhost:8082/ws-notifications` in local development.
- Subscribes to `/topic/notifications/{customerId}` after STOMP connects.
- Reconnects after a network interruption.
- Sends heartbeats.
- Closes the socket cleanly when the component unmounts.

Keep callback functions stable with `useCallback`. Passing new callback instances
on every render causes unnecessary disconnect and reconnect cycles.
