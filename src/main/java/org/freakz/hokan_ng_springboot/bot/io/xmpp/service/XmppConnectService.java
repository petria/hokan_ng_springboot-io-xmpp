package org.freakz.hokan_ng_springboot.bot.io.xmpp.service;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcEvent;
import org.freakz.hokan_ng_springboot.bot.common.events.IrcMessageEvent;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.Channel;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelStartupState;
import org.freakz.hokan_ng_springboot.bot.common.jpa.entity.ChannelState;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import rocks.xmpp.addr.Jid;
import rocks.xmpp.core.session.TcpConnectionConfiguration;
import rocks.xmpp.core.session.XmppClient;
import rocks.xmpp.core.stanza.model.Message;
import rocks.xmpp.extensions.muc.ChatRoom;
import rocks.xmpp.extensions.muc.ChatService;
import rocks.xmpp.extensions.muc.MultiUserChatManager;
import rocks.xmpp.extensions.muc.model.DiscussionHistory;
import rocks.xmpp.util.concurrent.AsyncResult;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<String, Jid> jidMap = new HashMap<>();

    private XmppClient xmppClient;

    private boolean isFirst = true;

    private Network getNetwork() {
        Network network = networkService.getNetwork(NETWORK_NAME);
        if (network == null) {
            network = new Network(NETWORK_NAME);
        }
        return networkService.save(network);
    }

    private Channel getChannel(String channelName) {
        Channel channel;
        channel = channelService.findByNetworkAndChannelName(getNetwork(), channelName);

        if (channel == null) {
            channel = channelService.createChannel(getNetwork(), channelName);
        }
        channel.setChannelStartupState(ChannelStartupState.JOIN);
        channel.setChannelState(ChannelState.JOINED);
        return channelService.save(channel);
    }

    private Channel getChannel(IrcEvent ircEvent) {
        return getChannel(ircEvent.getChannel());
    }

    private ChannelStats getChannelStats(Channel channel) {
        ChannelStats channelStats = channelStatsService.findFirstByChannel(channel);
        if (channelStats == null) {
            channelStats = new ChannelStats();
            channelStats.setChannel(channel);
        }
        return channelStats;
    }

    private User getUser(IrcEvent ircEvent) {
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

    private void connect() {

        log.debug("Connecting to: {}", configuration.getXmppServer());

        try {
            TcpConnectionConfiguration tcpConfiguration = TcpConnectionConfiguration.builder()
                    .hostname(configuration.getXmppServer())
                    .port(configuration.getXmppPort())
                    .build();
            xmppClient = XmppClient.create(configuration.getXmppServer(), tcpConfiguration);
            xmppClient.connect();
            xmppClient.login(configuration.getXmppLogin(), configuration.getXmppPassword());

            MultiUserChatManager multiUserChatManager = xmppClient.getManager(MultiUserChatManager.class);
            Collection<ChatService> chatServices = multiUserChatManager.discoverChatServices().getResult();
            xmppClient.addInboundMessageListener(e -> {
                Message message = e.getMessage();
                if (message.getBody() != null && message.getType() == Message.Type.CHAT && message.getBody().length() > 0) {
                    log.debug("message: {} -> {} ", message.getFrom().toString(), message.getBody());
                    sendMessageToEngine(message);
                }

            });
            ChatService chatService = chatServices.iterator().next();

            AsyncResult<List<ChatRoom>> listAsyncResult = chatService.discoverRooms();
            List<ChatRoom> chatRooms = listAsyncResult.get();
            ChatRoom botRoom = getBotRoom(chatRooms);
            if (botRoom != null) {
                botRoom.enter(configuration.getXmppUsername(), DiscussionHistory.forMaxMessages(1));
                botRoom.addInboundMessageListener(e -> {
                    if (isFirst) {
                        isFirst = false;
                        log.debug("Skip first, history message");
                    } else {
                        Message message = e.getMessage();
                        log.debug("Message: {}", message.getBody());
                        sendMessageToEngine(message);
                    }

                });

                Network nw = getNetwork();
                nw.addToConnectCount(1);
                nw.addToChannelsJoined(1);
                this.networkService.save(nw);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendMessageToEngine(Message xmppMessage) {

        String sender = xmppMessage.getFrom().toString();
        jidMap.put(sender, xmppMessage.getFrom());

        String message = xmppMessage.getBody();

        IrcLog ircLog = this.ircLogService.addIrcLog(new Date(), sender, CHANNEL_NAME, message);

        Network nw = getNetwork();
        nw.addToLinesReceived(1);
        this.networkService.save(nw);

        IrcMessageEvent ircEvent = new IrcMessageEvent("botName", NETWORK_NAME, CHANNEL_NAME, sender, "xmppLogin", "xmppHost", message);

        User user = getUser(ircEvent);
        Channel ch = getChannel(ircEvent);
        ChannelStats channelStats = getChannelStats(ch);
        channelStats.addToLinesReceived(1);
        channelStatsService.save(channelStats);

        UserChannel userChannel = userChannelService.getUserChannel(user, ch);
        if (userChannel == null) {
            userChannel = new UserChannel(user, ch);
        }
        userChannel.setLastIrcLogID(ircLog.getId() + "");
        userChannel.setLastMessageTime(new Date());
        userChannelService.save(userChannel);

        engineCommunicator.sendToEngine(ircEvent, null);

    }

    private ChatRoom getBotRoom(List<ChatRoom> chatRooms) {
        for (ChatRoom room : chatRooms) {
            if (room.getName().equals(configuration.getXmppRoom())) {
                return room;
            }
        }
        return null;
    }

    public void handleEngineResponse(EngineResponse response) {
        String sender = response.getIrcMessageEvent().getSender();
        Jid jid = jidMap.get(sender);
        Message message = new Message(jid, Message.Type.CHAT, response.getResponseMessage());
        xmppClient.sendMessage(message);
        Network network = getNetwork();
        network.addToLinesSent(1);
        networkService.save(network);

        ChannelStats channelStats = getChannelStats(getChannel(response.getIrcMessageEvent()));
        channelStats.setLastActive(new Date());
        channelStats.addToLinesSent(1);
        channelStatsService.save(channelStats);

    }

    @Override
    public void run(String... strings) throws Exception {
        connect();
    }

}