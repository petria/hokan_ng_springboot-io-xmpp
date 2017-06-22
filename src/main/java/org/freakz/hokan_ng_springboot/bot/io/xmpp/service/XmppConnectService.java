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
import rocks.xmpp.util.concurrent.AsyncResult;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Petri Airio
 */
@Service
@Slf4j
public class XmppConnectService implements CommandLineRunner {


    private static final String NETWORK_NAME = "xmppNetwork";
    private static final String CHANNEL_NAME = "xmppChannel";
    private static final String clientToken = "T6z2SAeREfTl4K1Lq8zUXh8RjPJuEDlS1JeGqMjI";
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

//    private Connection connection;

    //    private MultiUserChat multiUserChat;
    @Autowired
    private XmppConfiguration configuration;
    private LocalDateTime lastHandled = LocalDateTime.now();
    private ChatRoom joined = null;

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

        log.debug("Starting session...");

        try {

//            sendMessageToEngine("someone", "!wttr jyv");
            testRocks();


            while (true) {
                log.debug("Polling messages...");
/*                Message hipMessage = pollMessages();
                if (hipMessage == null) {
                    log.debug("Sleep ...");
                    Thread.sleep(5000L);
                    continue;
                }

                log.debug("Handling message: {}", hipMessage);
*/

                Object hipMessage = null;
                if (hipMessage == null) {
                    log.debug("Sleep ...");
                    Thread.sleep(5000L);
                    continue;
                }

//                log.debug(m.getFrom() + " " + m.getBody());
            }

//            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("Ended session...");
    }

    private void sendMessageToEngine(String sender, String message) {
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

    }

    private void testRocks() {
        String wttr = "Weather report: jyv, Jyv�skyl� Airport, Finland\n" +
                "\n" +
                "    \\  /       Partly cloudy\n" +
                "  _ /\"\".-.  9-11 °C        \n" +
                "    \\_( ).  ↘ 19 km/h      \n" +
                "    /(___(__)  10 km          \n" +
                "               0.0 mm         \n";

        try {
            TcpConnectionConfiguration tcpConfiguration = TcpConnectionConfiguration.builder()
                    .hostname(configuration.getXmppServer())
                    .port(5222)
                    .build();
            XmppClient xmppClient = XmppClient.create(configuration.getXmppServer(), tcpConfiguration);
            xmppClient.connect();
            xmppClient.login(configuration.getXmppLogin(), configuration.getXmppPassword());

            MultiUserChatManager multiUserChatManager = xmppClient.getManager(MultiUserChatManager.class);
            Collection<ChatService> chatServices = multiUserChatManager.discoverChatServices().getResult();


            ChatService chatService = chatServices.iterator().next();

            AsyncResult<List<ChatRoom>> listAsyncResult = chatService.discoverRooms();
            List<ChatRoom> chatRooms = listAsyncResult.get();
            ChatRoom botRoom = getBotRoom(chatRooms);
            if (botRoom != null) {
                botRoom.enter(configuration.getXmppUsername());
                joined = botRoom;
                botRoom.addInboundMessageListener(e -> {
                    Message message = e.getMessage();
                    sendMessageToEngine("someone", message.getBody());
//                    System.out.println(message.getFrom().getResource() + ": " + message.getBody());
                });
//                botRoom.sendMessage(wttr);
            }
            // 188553_bottest@conf.hipchat.com
            int foo = 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private ChatRoom getBotRoom(List<ChatRoom> chatRooms) {
        for (ChatRoom room : chatRooms) {
            if (room.getName().equals(configuration.getXmppRoom())) {
                return room;
            }
        }
        return null;
    }

/*    private Message pollMessages() throws FoxHttpException {
        HipChat hipChat = new HipChat(clientToken);
        ViewRoomHistory viewRoomHistory = new ViewRoomHistory(0, 100);
        viewRoomHistory.setReverse(false);
        ArrayList<Message> messages = hipChat.roomsApi().viewRoomHistory("3864731", viewRoomHistory).getItems();
        if (messages.size() > 0) {
            Message message = messages.get(0);
            String str = message.getDate();
            LocalDateTime dateTime = LocalDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME).plusHours(3);
            if (dateTime.isAfter(lastHandled)) {
                log.debug("New message: {}", message.getMessage());
                lastHandled = dateTime;
                return message;

            }
            int foo = 0;
        }
        return null;
    }*/

    @Override
    public void run(String... strings) throws Exception {
        connect();
    }

    public void handleEngineResponse(EngineResponse response) {
        if (joined != null) {
            joined.sendMessage(response.getResponseMessage());
        }
        int foo = 0;
/*        HipChat hipChat = null;
        try {
            hipChat = new HipChat(clientToken);
            hipChat.roomsApi().sendRoomMessage("3864731", new MessageRequestBody(response.getResponseMessage()));
        } catch (FoxHttpException e) {
            e.printStackTrace();

        } */
    }
}