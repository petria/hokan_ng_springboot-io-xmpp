package org.freakz.hokan_ng_springboot.bot.io.xmpp.jms;

import org.freakz.hokan_ng_springboot.bot.common.enums.HokanModule;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.jms.JmsEnvelope;
import org.freakz.hokan_ng_springboot.bot.common.jms.SpringJmsReceiver;
import org.freakz.hokan_ng_springboot.bot.io.xmpp.service.XmppConnectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by petria on 5.2.2015.
 * -
 */
@Component
public class IoXMPPJmsReceiver extends SpringJmsReceiver {

//    private final ConnectionManagerService connectionManagerService;

    @Autowired
    private XmppConnectService xmppConnectService;

    @Override
    public String getDestinationName() {
        return HokanModule.HokanIoXMPP.getQueueName();
    }

    @Override
    public void handleJmsEnvelope(JmsEnvelope envelope) throws Exception {
        if (envelope.getMessageIn().getPayLoadObject("ENGINE_RESPONSE") != null) {
            handleEngineReply(envelope);
        }
    }


    private void handleEngineReply(JmsEnvelope envelope) {
        EngineResponse response = (EngineResponse) envelope.getMessageIn().getPayLoadObject("ENGINE_RESPONSE");
        xmppConnectService.handleEngineResponse(response);
//        connectionManagerService.handleEngineResponse(response);
    }

}
