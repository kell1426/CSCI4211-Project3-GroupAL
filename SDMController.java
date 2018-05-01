package net.floodlightcontroller.EthernetLearning;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.*;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.*;
import org.projectfloodlight.openflow.types.*;
import net.floodlightcontroller.packet.Ethernet;
import org.projectfloodlight.openflow.util.*;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;

public class EthernetLearning implements IFloodlightModule, IOFMessageListener {

    private IFloodlightProviderService floodlightProvider;

    /*
        # PROJ3 Define your data structures here
    */
    public class Node {
      String switchName;
      OFPort port;
      Node next;

      public Node(String switchItem, OFPort portItem) {
        switchName = switchItem;
        port = portItem;
      }
    }
    static HashMap<String, Node> map = new HashMap<>();
    static HashMap<IPv4Address, MacAddress> ARPmap = new HashMap<>();
    IPv4Address ip1 = IPv4Addres.of("10.0.0.1");
    MacAddress mac1 = MacAddress.of("00:00:00:00:00:01");
    IPv4Address ip2 = IPv4Addres.of("10.0.0.2");
    MacAddress mac2 = MacAddress.of("00:00:00:00:00:02");
    IPv4Address ip3 = IPv4Addres.of("10.0.0.3");
    MacAddress mac3 = MacAddress.of("00:00:00:00:00:03");
    IPv4Address ip4 = IPv4Addres.of("10.0.0.4");
    MacAddress mac4 = MacAddress.of("00:00:00:00:00:04");
    IPv4Address ip5 = IPv4Addres.of("10.0.0.5");
    MacAddress mac5 = MacAddress.of("00:00:00:00:00:05");
    IPv4Address ip6 = IPv4Addres.of("10.0.0.6");
    MacAddress mac6 = MacAddress.of("00:00:00:00:00:06");
    ARPmap.put(ip1, mac1);
    ARPmap.put(ip2, mac2);
    ARPmap.put(ip3, mac3);
    ARPmap.put(ip4, mac4);
    ARPmap.put(ip5, mac5);
    ARPmap.put(ip6, mac6);

    /*
    h1: l1 port 3
    h2: l1 port 4
    s4: l1 port 1
    s4: l2 port 2
    /**
     * @param floodlightProvider the floodlightProvider to set
     */
    public void setFloodlightProvider(IFloodlightProviderService floodlightProvider) {
        this.floodlightProvider = floodlightProvider;
    }

    @Override
    public String getName() {
        return EthernetLearning.class.getPackage().getName();
    }

    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
    	switch (msg.getType()) {
        case PACKET_IN:
          Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
          //boolean portFound = false;
          if (eth.getEtherType() == EthType.ARP) {
            String switchMac = sw.getId().toString();
            OFPacketIn packetin_msg = (OFPacketIn) msg;
            OFPort port = packetin_msg.getInPort();
            IPv4 ipv4 = eth.getPayload();
            IPv4Address dstIP = ipv4.getDestinationAddress();
            MacAddress dstMac = ARPmap.get(dstIP);

            OFFactory myFactory = sw.getOFFactory();
            Match match = myFactory.buildMatch()
            .setExact(MatchField.ETH_TYPE, EthType.of(0x806))
            .build();

            ArrayList<OFAction> actionList = new ArrayList<OFAction>();
            OFActions actions = myFactory.actions();
            OFActionOutput output = actions.buildOutput()
            .setMaxLen(0xFFffFFff)
            .setPort(OFPort.CONTROLLER);
            .build();
            actionList.add(output);

            OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setHardTimeout(3600)
                .setIdleTimeout(10)
                .setPriority(32768)
                .setMatch(match)
                .setActions(actionList)
                .setTableId(TableId.of(1))
                .build();

            sw.write(flowAdd);
          }
          // MacAddress src = eth.getSourceMACAddress();
          // String srcString = src.toString();
          // MacAddress dst = eth.getDestinationMACAddress();
          // String dstString = dst.toString();
          // OFPort dstPort;
          // if(portFound) {
          //   //Tell switch to forward packet out this port
          //   //Install flow entry
          //   OFFactory myFactory = sw.getOFFactory();
          //   Match match = myFactory.buildMatch()
          //   .setExact(MatchField.ETH_DST, dst)
          //   .build();
          //
          //   ArrayList<OFAction> actionList = new ArrayList<OFAction>();
          //   OFActions actions = myFactory.actions();
          //   OFActionOutput output = actions.buildOutput()
          //   .setMaxLen(0xFFffFFff)
          //   .setPort(port)
          //   .build();
          //   actionList.add(output);
          //
          //   OFFlowAdd flowAdd = myFactory.buildFlowAdd()
          //       .setBufferId(OFBufferId.NO_BUFFER)
          //       .setHardTimeout(3600)
          //       .setIdleTimeout(10)
          //       .setPriority(32768)
          //       .setMatch(match)
          //       .setActions(actionList)
          //       .setTableId(TableId.of(1))
          //       .build();
          //
          //   sw.write(flowAdd);
          // }
          // else {
          //   //tell switch to flood packet on every
          //   //port except source port
          //   Ethernet l2 = new Ethernet();
          //   l2.setSourceMACAddress(src);
          //   l2.setDestinationMACAddress(MacAddress.BROADCAST);
          //
          //   byte[] serializedData = l2.serialize();
          //
          //   OFPacketOut po = sw.getOFFactory().buildPacketOut()
          //   .setData(serializedData)
          //   .setActions(Collections.singletonList((OFAction) sw.getOFFactory()
          //     .actions().output(OFPort.FLOOD, 0xffFFffFF)))
          //   .setInPort(OFPort.CONTROLLER)
          //   .build();
          //   sw.write(po);
          // }
          break;

        /*
        *
          # PROJ3 Your logic goes here
        *
        */
        default:
            break;
        }

        return Command.CONTINUE;
    }



    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    // IFloodlightModule

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        // We don't provide any services, return null
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService>
            getServiceImpls() {
        // We don't provide any services, return null
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>>
            getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l =
                new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context)
            throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }
}





/*case ARP IN:

get src port
get src Mac
get src IP
get dst IP

convert dst IP to Mac

create ARP response with dst Mac

write to switch*/
