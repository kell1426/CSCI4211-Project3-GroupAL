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
          boolean portFound = false;
          String switchMac = sw.getId().toString();
          OFPacketIn packetin_msg = (OFPacketIn) msg;
          OFPort port = packetin_msg.getInPort();
          MacAddress src = eth.getSourceMACAddress();
          String srcString = src.toString();
          MacAddress dst = eth.getDestinationMACAddress();
          String dstString = dst.toString();
          OFPort dstPort;
          if(map.containsKey(srcString)) {
            Node n = map.get(srcString);
            Node prev_n = new Node(null, null);
            while(n != null) {
              if(n.switchName == switchMac) {
                if(n.port == null) {
                  n.port = port;
                  portFound = true;
                  break;
                }
              }
              prev_n = n;
              n = n.next;
            }
            if(!portFound)
            {
              Node temp = new Node(switchMac, port);
              temp.next = null;
              prev_n.next = temp;
            }
          }
          else {
            Node n = new Node(switchMac, port);
            map.put(srcString, n);
          }

          portFound = false;
          if(map.containsKey(dstString)) {
            Node n = map.get(dstString);
            Node prev_n;
            while(n != null) {
              if(n.switchName == switchMac) {
                if(n.port != null) {
                  dstPort = n.port;
                  portFound = true;
                  break;
                }
              }
              prev_n = n;
              n = n.next;
            }
            if(portFound) {
              //Tell switch to forward packet out this port
              //Install flow entry
              // OFMatch match = new OFMatch();
              // match.setWildcards(Wildcards.FULL.matchOn(Flag.DL_TYPE).matchOn(Flag.NW_DST).withNwDstMask(24));
              // match.setDataLayerType(Ethernet.TYPE_MacAddress);
              OFFactory myFactory = sw.getOFFactory();
              Match match = myFactory.buildMatch()
              .setExact(MatchField.ETH_DST, dst)
              .build();

              ArrayList<OFAction> actionList = new ArrayList<OFAction>();
              //OFActionOutput action = new OFActionOutput().setPort(port);
              //OFActionNetworkLayerSource ofanls = new OFActionNetworkLayerSource();
              OFActions actions = myFactory.actions();
              OFActionOutput output = actions.buildOutput()
              .setMaxLen(0xFFffFFff)
              .setPort(port)
              .build();
              actionList.add(output);

              // OFFlowMod flowMod = new OFFlowMod();
              // flowMod.setMatch(match);
              // flowMod.setActions(actions);
              // flowMod.setLength(OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH
              //   + OFActionNetworkLayerSource.MINIMUM_LENGTH);


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
              // File log = new File("log.txt");
              // FileWriter writer;
              // writer = new FileWriter(log, true);
              // PrintWriter printer = new PrintWriter(writer);
              // printer.appened("Added flow mod\n");
              // printer.close();
              System.out.println("Installed Flow Entry");
            }
          }
          else {
            //tell switch to flood packet on every
            //port except source port
            Ethernet l2 = new Ethernet();
            l2.setSourceMACAddress(src);
            l2.setDestinationMACAddress(MacAddress.BROADCAST);

            byte[] serializedData = l2.serialize();

            OFPacketOut po = sw.getOFFactory().buildPacketOut()
            .setData(serializedData)
            .setActions(Collections.singletonList((OFAction) sw.getOFFactory()
              .actions().output(OFPort.FLOOD, 0xffFFffFF)))
            .setInPort(OFPort.CONTROLLER)
            .build();
            sw.write(po);
            // File log = new File("log.txt");
            // FileWriter writer;
            // writer = new FileWriter(log, true);
            // PrintWriter printer = new PrintWriter(writer);
            // printer.appened("Flooded packet\n");
            // printer.close();
            System.out.println("Flooded packet");
          }
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
