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
import org.freakz.hokan_ng_springboot.bot.io.xmpp.config.XmppConfiguration;
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

    @Autowired
    private XmppConfiguration configuration;

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

//        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        log.debug("Starting session...");
        try {
            ConnectionConfiguration config = new ConnectionConfiguration(configuration.getXmppServer(), configuration.getXmppPort());
            connection = new XMPPConnection(config);
            log.debug("Connecting to : " + connection.getHost() + ":" + connection.getPort());
            // Connect to the server
            connection.connect();
            connection.login(configuration.getXmppLogin(), configuration.getXmppPassword());


            multiUserChat = new MultiUserChat(connection, configuration.getXmppRoom());
            DiscussionHistory discussionHistory = new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);
            discussionHistory.setMaxChars(0);
            discussionHistory.setSeconds(1);
            discussionHistory.setSince(new Date());
            String username = configuration.getXmppUsername();
            multiUserChat.join(username, null, discussionHistory, 10000L);
            PacketCollector packetCollector = null;
            packetCollector = connection.createPacketCollector(new PacketTypeFilter(Message.class));
            String version = SmackConfiguration.getVersion();
            SmackConfiguration
            while (true) {
//                Message m = (Message) packetCollector.nextResult(1000L);
                Message m = null;
                try {
                    m = (Message) packetCollector.nextResult();
                } catch (Exception e) {
                    int foo = 0;
                }
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (m == null) {
                    continue;
                }
                String message = m.getBody();
                if (message == null) {
                    continue;
                }
                String sender = m.getFrom();
                log.debug("sender: {}", sender);
                if (sender.endsWith(username)) {
                    // don't handle own messages
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
                log.debug(m.getFrom() + " " + m.getBody());
            }

            connection.disconnect();
        } catch (XMPPException e) {
            e.printStackTrace();
        }
        log.debug("Ended session...");
    }

    @Override
    public void run(String... strings) throws Exception {
        connect();
    }

    public void handleEngineResponse(EngineResponse response) {
        Message m = new Message(multiUserChat.getRoom(), Message.Type.groupchat);
        m.setBody(response.getResponseMessage());
        connection.sendPacket(m);
    }
}