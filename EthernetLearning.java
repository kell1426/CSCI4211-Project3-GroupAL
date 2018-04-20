// EthernetLearning.java
// Daniel Kelly and Noah Bolduan
// 4/20/18
// CSCI 4211 Project 3

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
    //Linked list node. The linked list maps port numbers
    //to each switch.
    public class Node {
      String switchName;
      OFPort port;
      Node next;

      public Node(String switchItem, OFPort portItem) {
        switchName = switchItem;
        port = portItem;
      }
    }
    //A hashmap that maps destination MAC addresses to a
    //linked list of switch/port pairings.
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
          /*If the map has an entry for the source MAC address,
          scan the linked list for a node for the current switch.
          If a match is found, use that port. If not, add a node to
          map the MAC address to a port.
          If the map does not have an entry for the source MAC
          address, create a linked list node and add it to the map.*/
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
            if(!portFound){
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
          /*If the map has an entry for the destination MAC address,
          scan the linked list for an entry for the current switch.
          If an entry is found, tell the switch to use that port and
          to install a flow entry. If there is no entry for the
          switch, tell the switch to send a flood message. */
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
              OFFactory myFactory = sw.getOFFactory();
              Match match = myFactory.buildMatch()
              .setExact(MatchField.ETH_DST, dst)
              .build();

              ArrayList<OFAction> actionList = new ArrayList<OFAction>();
              OFActions actions = myFactory.actions();
              OFActionOutput output = actions.buildOutput()
              .setMaxLen(0xFFffFFff)
              .setPort(port)
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
