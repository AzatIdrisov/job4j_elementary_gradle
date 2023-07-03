package ru.job4j.pooh;

import java.util.concurrent.*;

public class QueueSchema implements Schema {
    private final CopyOnWriteArrayList<Receiver> receivers = new CopyOnWriteArrayList<Receiver>();
    private final ConcurrentHashMap<String, BlockingQueue<String>> data = new ConcurrentHashMap<>();
    private final Condition condition = new Condition();

    @Override
    public void addReceiver(Receiver receiver) {
        receivers.add(receiver);
    }

    @Override
    public void publish(Message message) {
        data.putIfAbsent(message.name(), new LinkedBlockingQueue<>());
        data.get(message.name()).add(message.text());
        condition.on();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                for (var receiver : receivers) {
                    var message = data.get(receiver.name()).poll();
                    if (message == null) {
                        continue;
                    }
                    receiver.receive(
                            message
                    );
                }
                condition.off();
                condition.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}