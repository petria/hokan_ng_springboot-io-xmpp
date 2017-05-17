package org.freakz.hokan_ng_springboot.bot.io.xmpp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by Petri Airio on 17.5.2017.
 */
@Configuration
@PropertySource("file:xmpp.properties")
@Getter
@Setter
public class XmppConfiguration {

    @Value("${xmpp.server}")
    private String xmppServer;

    @Value("${xmpp.port}")
    private int xmppPort;

    @Value("${xmpp.username}")
    private String xmppUsername;

    @Value("${xmpp.login}")
    private String xmppLogin;

    @Value("${xmpp.password}")
    private String xmppPassword;

    @Value("${xmpp.room}")
    private String xmppRoom;

    public void setXmppServer(String xmppServer) {
        this.xmppServer = xmppServer;
    }

}
