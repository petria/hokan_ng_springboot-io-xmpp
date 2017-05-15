package org.freakz.hokan_ng_springboot.bot.io.xmpp.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.io.xmpp.jms.EngineCommunicator;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author Petri Airio
 */
@Service
@Slf4j
public class XmppConnectService implements CommandLineRunner {

    @Autowired
    private EngineCommunicator engineCommunicator;

    public void connect() {

        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        System.out.println("Starting session...");
        try {
            // Create a connection to the igniterealtime.org XMPP server.
            String server = "chat.hipchat.com";
            int port = 5222;


            //ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
            ConnectionConfiguration config = new ConnectionConfiguration(server, port);
            Connection connection = new XMPPConnection(config);
            System.out.println("Connecting to : " + connection.getHost() + ":" + connection.getPort());
            // Connect to the server
            connection.connect();
            // Most servers require you to login before performing other tasks.
//            String username = "707396_4968751@chat.hipchat.com";
//            String password = "poiASD098?!?";
            String username = "707396_4968738@chat.hipchat.com";
            String password = "iFdCEHnBpJ6cBc";

            // will receive the message sent.
            String receiver = "707396_bottest";
            connection.login(username, password);

            ChatManager cm = connection.getChatManager();
            Roster roster = connection.getRoster();
//            RoomInfo roomInfo = MultiUserChat.getRoomInfo(connection, "707396_robbottitesti@conf.hipchat.com");
            MultiUserChat multiUserChat = new MultiUserChat(connection, "707396_robbottitesti@conf.hipchat.com");
            DiscussionHistory discussionHistory = new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);
            discussionHistory.setMaxChars(0);
            discussionHistory.setSeconds(1);
            discussionHistory.setSince(new Date());
            multiUserChat.join("Petri Airio", null, discussionHistory, 10000L);
            PacketCollector packetCollector = null;
            packetCollector = connection.createPacketCollector(new PacketTypeFilter(Message.class));
            int foo = 0;
            while (true) {
                Message m = (Message) packetCollector.nextResult();
                IrcMessageEvent ircMessageEvent = new IrcMessageEvent();
                ircMessageEvent.setMessage(m.getBody());
                ircMessageEvent.setTimestamp(System.currentTimeMillis());
                ircMessageEvent.setWebMessage(true);
                engineCommunicator.sendToEngine(ircMessageEvent, null);

                System.out.println(m.getFrom() + " " + m.getBody());

                if (foo != 0) {
                    break;
                }

            }

            connection.disconnect();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        System.out.println("Ended session...");
    }

    @Override
    public void run(String... strings) throws Exception {
        this.connect();
    }
}