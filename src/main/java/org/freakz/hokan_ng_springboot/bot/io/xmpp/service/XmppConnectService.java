package org.freakz.hokan_ng_springboot.bot.io.xmpp.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcEvent;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.Channel;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelStats;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.IrcLog;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.Network;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.User;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.UserChannel;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.ChannelService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.ChannelStatsService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.IrcLogService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.NetworkService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.UserChannelService;
import org.freakz.hokan_ng_springboot.bot.common.jpa.service.UserService;
import org.freakz.hokan_ng_springboot.bot.common.util.StringStuff;
import org.freakz.hokan_ng_springboot.bot.io.xmpp.jms.EngineCommunicator;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
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


    private static final String NETWORK_NAME = "xmppNetwork";
    private static final String CHANNEL_NAME = "xmppChannel";

    @Autowired
    private EngineCommunicator engineCommunicator;

    @Autowired
    private ChannelService channelService;

    @Autowired
    private ChannelStatsService channelStatsService;

    @Autowired
    private IrcLogService ircLogService;

    @Autowired
    private NetworkService networkService;

    @Autowired
    private UserChannelService userChannelService;

    @Autowired
    private UserService userService;

    private Connection connection;

    private MultiUserChat multiUserChat;

    public Network getNetwork() {
        Network network = networkService.getNetwork(NETWORK_NAME);
        if (network == null) {
            network = new Network(NETWORK_NAME);
            network = networkService.save(network);
        }
        return network;
    }

    public Channel getChannel(String channelName) {
        Channel channel;
        channel = channelService.findByNetworkAndChannelName(getNetwork(), channelName);

        if (channel == null) {
            channel = channelService.createChannel(getNetwork(), channelName);
        }
        return channel;
    }

    public Channel getChannel(IrcEvent ircEvent) {
        return getChannel(ircEvent.getChannel());
    }

    public ChannelStats getChannelStats(Channel channel) {
        ChannelStats channelStats = channelStatsService.findFirstByChannel(channel);
        if (channelStats == null) {
            channelStats = new ChannelStats();
            channelStats.setChannel(channel);
        }
        return channelStats;
    }

    public UserChannel getUserChannel(User user, Channel channel, IrcLog ircLog) {
        UserChannel userChannel = userChannelService.getUserChannel(user, channel);
        if (userChannel == null) {
            userChannel = userChannelService.createUserChannel(user, channel, ircLog);
        }
        return userChannel;
    }

    public User getUser(IrcEvent ircEvent) {
        User user;
        User maskUser = this.userService.getUserByMask(ircEvent.getMask());
        if (maskUser != null) {
            user = maskUser;
        } else {
            user = this.userService.findFirstByNick(ircEvent.getSender());
            if (user == null) {
                user = new User(ircEvent.getSender());
                user = userService.save(user);
            }
        }
        user.setRealMask(StringStuff.quoteRegExp(ircEvent.getMask()));
        this.userService.save(user);
        return user;
    }


    public void connect() {

        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        System.out.println("Starting session...");
        try {
            // Create a connection to the igniterealtime.org XMPP server.
            String server = "chat.hipchat.com";
            int port = 5222;


            //ConnectionConfiguration config = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
            ConnectionConfiguration config = new ConnectionConfiguration(server, port);
            connection = new XMPPConnection(config);
            System.out.println("Connecting to : " + connection.getHost() + ":" + connection.getPort());
            // Connect to the server
            connection.connect();
            // Most servers require you to login before performing other tasks.
            String username = "707396_4968751@chat.hipchat.com";
            String password = "poiASD098?!?";
//            String username = "707396_4968738@chat.hipchat.com";
//            String password = "iFdCEHnBpJ6cBc";

            connection.login(username, password);


            multiUserChat = new MultiUserChat(connection, "707396_robbottitesti@conf.hipchat.com");
            DiscussionHistory discussionHistory = new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);
            discussionHistory.setMaxChars(0);
            discussionHistory.setSeconds(1);
            discussionHistory.setSince(new Date());
            String myNick = "Hokan TheBot";
            multiUserChat.join(myNick, null, discussionHistory, 10000L);
            PacketCollector packetCollector = null;
            packetCollector = connection.createPacketCollector(new PacketTypeFilter(Message.class));
            int foo = 0;
            while (true) {
                Message m = (Message) packetCollector.nextResult();
                String message = m.getBody();
                if (m == null || message == null) {
                    continue;
                }
                //(String botNick, String network, String channel, String sender, String login, String hostname, String message)
                String sender = m.getFrom();
                log.debug("sender: {}", sender);
                if (sender.endsWith(myNick)) {
                    continue;
                }
                IrcLog ircLog = this.ircLogService.addIrcLog(new Date(), sender, CHANNEL_NAME, message);

                Network nw = getNetwork();
                nw.addToLinesReceived(1);
                this.networkService.save(nw);

                IrcMessageEvent ircEvent = new IrcMessageEvent("botName", NETWORK_NAME, CHANNEL_NAME, sender, "xmppLogin", "xmppHost", message);

                User user = getUser(ircEvent);
                Channel ch = getChannel(ircEvent);

                UserChannel userChannel = userChannelService.getUserChannel(user, ch);
                if (userChannel == null) {
                    userChannel = new UserChannel(user, ch);
                }
                userChannel.setLastIrcLogID(ircLog.getId() + "");
                userChannel.setLastMessageTime(new Date());
                userChannelService.save(userChannel);


                engineCommunicator.sendToEngine(ircEvent, null);

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
//        Thread t = new Thread(this::connect);
//        t.start();
        connect();
    }

    public void handleEngineResponse(EngineResponse response) {
        Message m = new Message(multiUserChat.getRoom(), Message.Type.groupchat);
        m.setBody(response.getResponseMessage());
        connection.sendPacket(m);
    }
}