package io.nadron.example.zombieclient;

import io.nadron.client.app.Session;
import io.nadron.client.app.impl.SessionFactory;
import io.nadron.client.communication.NettyMessageBuffer;
import io.nadron.client.event.Event;
import io.nadron.client.event.impl.AbstractSessionEventHandler;
import io.nadron.client.util.LoginHelper;
import io.nadron.example.zombie.domain.IAM;
import io.nadron.example.zombie.domain.ZombieCommands;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Artem Zhdanov <azhdanov@griddynamics.com>
 * @since 26.08.2016
 */
public class SimpleZombieClient {
    private final static Integer USER_NUM = 2;
    private static ScheduledExecutorService taskExecutor = Executors.newSingleThreadScheduledExecutor();

    private static class InBoundHandler extends AbstractSessionEventHandler {
        private final ScheduledFuture taskRef;
        public InBoundHandler(final Session session, final ScheduledFuture taskRef) {
            super(session);
            this.taskRef = taskRef;
        }

        @Override
        public void onDataIn(Event event)
        {
            NettyMessageBuffer buffer = (NettyMessageBuffer)event.getSource();
            if(buffer.readableBytes()>=4) {
                int cmd = buffer.readInt();
                ZombieCommands command = ZombieCommands.CommandsEnum.fromInt(cmd);
                if (command == ZombieCommands.APOCALYPSE) {
                    System.out.println("Closing session " + session.getId()  + " timer due to apocalypse");
                    taskRef.cancel(false);
                    session.close();
                }
                else {
                    System.out.println("Remaining Human Population: " + cmd);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {

        for (int i = 0; i < USER_NUM; i++) {
            LoginHelper.LoginBuilder builder = new LoginHelper.LoginBuilder().username("user" + i)
                    .password("pass"+i).connectionKey("Zombie_ROOM_1")
                    .nadronTcpHostName("localhost").tcpPort(18090)
                    .nadronUdpHostName("localhost").udpPort(50122); // u can not

            LoginHelper loginHelper = builder.build();
            SessionFactory sessionFactory = new SessionFactory(loginHelper);
            Session session = sessionFactory.createAndConnectSession();

            GamePlay task = null;
            if ((i % 2) == 0)
            {
                task = new GamePlay(IAM.DEFENDER, session);
            } else
            {
                task = new GamePlay(IAM.ZOMBIE, session);
            }
            ScheduledFuture taskRef =
                    taskExecutor.scheduleAtFixedRate(task, 2000, 200,
                            TimeUnit.MILLISECONDS);
            AbstractSessionEventHandler handler = new SimpleZombieClient.InBoundHandler(session, taskRef);
            session.addHandler(handler);
        }


    }
}
