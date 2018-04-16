package net.floodlightcontroller.EthernetLearning;

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
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;


public class EthernetLearning implements IFloodlightModule, IOFMessageListener {

    private IFloodlightProviderService floodlightProvider;

    /*
        # PROJ3 Define your data structures here
    */
    public class Node {
      DatapathId switchName;
      int interface;
      Node next;

      public Node(String switchItem, int interfaceItem) {
        switchName = switchItem;
        interface = interfaceItem;
      }
    }
    static HashMap<MacAddress, Node> map = new HashMap<>();
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
          boolean interfaceFound = false;
          DatapathId switchMac = sw.getId()
          int interface = packetin_msg.getInPort()
          MacAddress src = eth.getSourceMACAddress();
          MacAddress dst = eth.getDestinationMACAddress();
          int dstInterface;
          if(map.containsKey(src)) {
            Node n = map.get(src);
            Node prev_n;
            while(n != NULL) {
              if(n.switchName == switchMac) {
                if(n.interface == NULL) {
                  n.interface = interface;
                  interfaceFound = true;
                  break;
                }
              }
              prev_n = n;
              n = n.next
            }
            if(!interfaceFound)
            {
              Node temp = new Node(switchMac, interface);
              temp.next = NULL;
              prev_n.next = temp;
            }
          }
          else {
            Node n = new Node(switchMac, interface);
            map.put(src, n);
          }

          interfaceFound = false;
          if(map.containskey(dst)) {
            Node n = map.get(dst);
            Node prev_n;
            while(n != NULL) {
              if(n.switchName == switchMac) {
                if(n.interface != NULL) {
                  dstInterface = n.interface;
                  interfaceFound = true;
                  break;
                }
              }
              prev_n = n;
              n = n.next;
            }
            if(interfaceFound) {
              //Tell switch to forward packet out this interface
              //Install flow entry
              OFMatch match = new OFMatch();
              match.setWildcards(Wildcards.FULL.matchOn(Flag.DL_TYPE).matchOn(Flag.NW_DST).withNwDstMask(24));
              match.setDataLayerType(Ethernet.TYPE_MacAddress);
            }
            else {
              //tell switch to flood packet on every
              //interface except source interface

            }
          }

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
