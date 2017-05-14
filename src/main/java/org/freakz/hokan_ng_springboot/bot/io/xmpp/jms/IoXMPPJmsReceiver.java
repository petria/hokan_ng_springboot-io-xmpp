package org.freakz.hokan_ng_springboot.bot.io.xmpp.jms;

import lombok.extern.slf4j.Slf4j;
import org.freakz.hokan_ng_springboot.bot.common.events.EngineResponse;
import org.freakz.hokan_ng_springboot.bot.common.events.NotifyRequest;
import org.freakz.hokan_ng_springboot.bot.common.jms.JmsEnvelope;
import org.freakz.hokan_ng_springboot.bot.common.jms.SpringJmsReceiver;
import org.freakz.hokan_ng_springboot.bot.common.service.ConnectionManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by petria on 5.2.2015.
 * -
 */
@Component
@Slf4j
public class IoXMPPJmsReceiver extends SpringJmsReceiver {

//    private final ConnectionManagerService connectionManagerService;


    @Override
    public String getDestinationName() {
        return "HokanNGIoXMPPQueue";
    }

    @Override
    public void handleJmsEnvelope(JmsEnvelope envelope) throws Exception {
        if (envelope.getMessageIn().getPayLoadObject("ENGINE_RESPONSE") != null) {
            handleEngineReply(envelope);
        }
    }


    private void handleEngineReply(JmsEnvelope envelope) {
        EngineResponse response = (EngineResponse) envelope.getMessageIn().getPayLoadObject("ENGINE_RESPONSE");
//        connectionManagerService.handleEngineResponse(response);
    }

}
