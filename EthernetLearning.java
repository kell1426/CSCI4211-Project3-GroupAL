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
    // class SwitchTable {
    //   String switchName;
    //   int interface;
    //   public SwitchTable(String switchName, int interface) {
    //     this.switch = switchName;
    //     this.interface = interface;
    //   }
    // }
    // List<SwitchTable> list = new LinkedList<>();
    public class Node {
      String switch;
      int interface;
      Node next;

      public Node(String switchItem, int interfaceItem) {
        switch = switchItem;
        interface = interfaceItem;
      }
    }

    public class LinkedList {
      Node head;

      public LinkedList(String switchItem, int interfaceItem) {
        head = new Node(switchItem, interfaceItem);
      }

      public void add(String switchItem, int interfaceItem) {
        temp = new Node(switchItem, interfaceItem);
        temp.next = head.next;
        head.next = temp;
      }
    }
    static HashMap<String, LinkedList> map = new HashMap<>();
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
          String src = eth.getSourceMACAddress();
          if(map.containsKey(src)) {
            LinkedList n = map.get(src);
            LinkedList prev_n;
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
