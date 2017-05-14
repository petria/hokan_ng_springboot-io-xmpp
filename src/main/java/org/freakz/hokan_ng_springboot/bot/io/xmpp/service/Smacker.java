package org.freakz.hokan_ng_springboot.bot.io.xmpp.service;

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
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 * @author pjn
 */
public class Smacker {

    /**
     * @param args
     */
    public static void main(String[] args) {

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
            multiUserChat.join("Petri Airio");
            PacketCollector packetCollector = null;
            packetCollector = connection.createPacketCollector(new PacketTypeFilter(Message.class));
            int foo = 0;
            while (true) {
                Message m = (Message) packetCollector.nextResult();

                System.out.println(m.getFrom() + " " + m.getBody());

                if (foo != 0) {
                    break;
                }

            }
/*            connection.getChatManager().addChatListener(new ChatManagerListener() {

                public void chatCreated(final Chat chat, final boolean createdLocally) {
                    chat.addMessageListener(new MessageListener() {
                        public void processMessage(Chat chat, Message message) {
                            System.out.println("Received message: "
                                    + (message != null ? message.getBody() : "NULL"));
                        }
                    });
                }
            });*/
/*            String user = con.getUser();
            Chat chat = cm.createChat(receiver, new MessageListener() {
                public void processMessage(Chat chat, Message message) {
                    System.out.println("Received message: " + message);
                }
            });

            chat.sendMessage("Smack> Message sent via API.");
*/
            //Thread.currentThread();
//            Thread.sleep(30000);
            // Disconnect from the server
            connection.disconnect();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        System.out.println("Ended session...");
    }

}