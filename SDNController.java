package net.floodlightcontroller.EthernetLearning;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.*;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.*;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.IPv6;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.routing.ForwardingBase;

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
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.util.*;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.python.google.common.collect.ImmutableList;

public class EthernetLearning implements IFloodlightModule, IOFMessageListener {

    protected static boolean REMOVE_FLOWS_ON_LINK_OR_PORT_DOWN;
    private IFloodlightProviderService floodlightProvider;
    protected ILinkDiscoveryService linkDiscoverer;
    protected IOFSwitchService switchService;

    public int s4Counter = 0; //Counter of link failures to S4
    public int s5Counter = 0; //Counter of link failures to S5
    public int priority = 1;  //priority counter that is incremented to give
                              //newer flow entries higher priority over old ones

    //Maps the IPv4 addresses and MAC addresses of the hosts
    static public HashMap<IPv4Address, MacAddress> ARPmap = new HashMap<>();

    //Array storing the DatapathId's of the switches
    public DatapathId[] switches = new DatapathId[5];

    //fill switch DatapathId array
    public void switchInitializer(DatapathId[] switches){
      switches[0] = DatapathId.of("00:00:00:00:00:00:00:01"); //L1
      switches[1] = DatapathId.of("00:00:00:00:00:00:00:02"); //L2
      switches[2] = DatapathId.of("00:00:00:00:00:00:00:03"); //L3
      switches[3] = DatapathId.of("00:00:00:00:00:00:00:04"); //S4
      switches[4] = DatapathId.of("00:00:00:00:00:00:00:05"); //S5
    }

    //Fill ARPmap of host IP and MAC addresses
    public void ARPinitializer(HashMap<IPv4Address, MacAddress> ARPmap) {
      IPv4Address ip1 = IPv4Address.of("10.0.0.1");
      MacAddress mac1 = MacAddress.of("00:00:00:00:00:01");
      IPv4Address ip2 = IPv4Address.of("10.0.0.2");
      MacAddress mac2 = MacAddress.of("00:00:00:00:00:02");
      IPv4Address ip3 = IPv4Address.of("10.0.0.3");
      MacAddress mac3 = MacAddress.of("00:00:00:00:00:03");
      IPv4Address ip4 = IPv4Address.of("10.0.0.4");
      MacAddress mac4 = MacAddress.of("00:00:00:00:00:04");
      IPv4Address ip5 = IPv4Address.of("10.0.0.5");
      MacAddress mac5 = MacAddress.of("00:00:00:00:00:05");
      IPv4Address ip6 = IPv4Address.of("10.0.0.6");
      MacAddress mac6 = MacAddress.of("00:00:00:00:00:06");
      ARPmap.put(ip1, mac1);
      ARPmap.put(ip2, mac2);
      ARPmap.put(ip3, mac3);
      ARPmap.put(ip4, mac4);
      ARPmap.put(ip5, mac5);
      ARPmap.put(ip6, mac6);
    }

    //Create flow entries for switches
    public void FlowTableInitilizer()
    {
      //Create an entry in each switch to send ARP messages to the controller
      for(int i = 0; i < 5; i++){
        IOFSwitch sw = switchService.getSwitch(switches[i]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        .setExact(MatchField.ETH_TYPE, EthType.of(0x806)) //ARP Type in Hex
        .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFActionOutput output = actions.buildOutput()
        .setMaxLen(0xFFffFFff)
        .setPort(OFPort.CONTROLLER)
        .build();
        actionList.add(output);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
            .setBufferId(OFBufferId.NO_BUFFER)
            .setPriority(32768)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }
      //Create flow entries for leaf switches to send traffic to hosts based on
      //the destination MAC address. These entries are given a higher priority,
      //so packets destined for a connected host are not sent to spine switches.
      for(int i = 0; i < 6; i++){
        int switchnum = 0; //Switch that the flow entry is being made for
        if(i < 2){
          switchnum = 0;
        }
        else if(i < 4){
          switchnum = 1;
        }
        else if (i < 6){
          switchnum = 2;
        }
        //MAC Addresses of hosts
        String[] MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:02",
                                "00:00:00:00:00:03", "00:00:00:00:00:04",
                                "00:00:00:00:00:05", "00:00:00:00:00:06"};
        //Get the current switch with switchnum
        IOFSwitch sw =switchService.getSwitch(switches[(switchnum)]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        //Match the destination MAC address
        .setExact(MatchField.ETH_DST, MacAddress.of(MacAddr[i]))
        .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFActionOutput output = actions.buildOutput()
        .setMaxLen(0xFFffFFff)
        //Set output port based on the destination host
        .setPort(OFPort.of((i % 2) + 3))
        .build();
        actionList.add(output);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
            .setBufferId(OFBufferId.NO_BUFFER)
            .setPriority(32768)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }
      //Create flow entries from leaf switches to spine switches based on the
      //source MAC address. These flow entries are given a lower priority, so
      //packets destined for hosts connected to the leaf switches are not sent
      //to the spine switches.
      for(int i = 0; i < 6; i++){
        int switchnum = 0; //Switch that the flow entry is being made for
        if(i < 2){
          switchnum = 0;
        }
        else if(i < 4){
          switchnum = 1;
        }
        else if (i < 6){
          switchnum = 2;
        }
        //MAC addresses of hosts
        String[] MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:02",
                                "00:00:00:00:00:03", "00:00:00:00:00:04",
                                "00:00:00:00:00:05", "00:00:00:00:00:06"};
        IOFSwitch sw = switchService.getSwitch(switches[(switchnum)]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        //Match the source MAC address
        .setExact(MatchField.ETH_SRC, MacAddress.of(MacAddr[i]))
        .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFActionOutput output = actions.buildOutput()
        .setMaxLen(0xFFffFFff)
        //Set output port based on the source host
        .setPort(OFPort.of((i % 2) + 1))
        .build();
        actionList.add(output);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
            .setBufferId(OFBufferId.NO_BUFFER)
            .setPriority(10000)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }
      //Create flow entries from spine switches to leaf switches based on the
      //destination MAC address.
      for(int i = 0; i < 6; i++){
        int switchnum = 0; //Switch that the flow entry is being made for
        if(i < 3){
          switchnum = 3;
        }
        else if(i < 6){
          switchnum = 4;
        }
        String[] MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:03",
                                "00:00:00:00:00:05", "00:00:00:00:00:02",
                                "00:00:00:00:00:04", "00:00:00:00:00:06"};
        IOFSwitch sw =switchService.getSwitch(switches[switchnum]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        //Match the destination MAC address
        .setExact(MatchField.ETH_DST, MacAddress.of(MacAddr[i]))
        .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFActionOutput output = actions.buildOutput()
        .setMaxLen(0xFFffFFff)
        //Set output port based on the destination host
        .setPort(OFPort.of((i % 3) + 1))
        .build();
        actionList.add(output);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
            .setBufferId(OFBufferId.NO_BUFFER)
            .setPriority(32768)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }
    }

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

    //Process incoming ARP messages
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
    	switch (msg.getType()) {
        case PACKET_IN:
          Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
          //If the packet is not an ARP message, ignore it
          if (eth.getEtherType() == EthType.ARP) {
            String switchMac = sw.getId().toString();
            OFPacketIn packetin_msg = (OFPacketIn) msg;
            OFPort port = packetin_msg.getInPort();
            IPv4 ipv4 = new IPv4();
            eth.setPayload(ipv4);
            IPv4Address srcIP = ipv4.getSourceAddress();
            IPv4Address dstIP = ipv4.getDestinationAddress();
            MacAddress srcMac = eth.getSourceMACAddress();
            MacAddress dstMac = ARPmap.get(dstIP);

            //Set the source and destination MAC addresses
            Ethernet l2 = new Ethernet();
            l2.setSourceMACAddress(dstMac);
            l2.setDestinationMACAddress(srcMac);
            l2.setEtherType(EthType.ARP);

            //Set the source and destination IP addresses
            IPv4 l3 = new IPv4();
            l3.setSourceAddress(dstIP);
            l3.setDestinationAddress(srcIP);
            l3.setTtl((byte) 64);
            l3.setProtocol(IpProtocol.UDP);

            UDP l4 = new UDP();
            l4.setSourcePort(TransportPort.of(65003));
            l4.setDestinationPort(TransportPort.of(67));

            Data l7 = new Data();
            l7.setData(new byte[1000]);

            l2.setPayload(l3);
            l3.setPayload(l4);
            l4.setPayload(l7);


            byte[] serializedData = l2.serialize();

            //Build and send packet
            OFPacketOut po = sw.getOFFactory().buildPacketOut()
            .setData(serializedData)
            .setActions(Collections.singletonList((OFAction) sw.getOFFactory().actions().output(port, 0xffFFffFF)))
            .setInPort(OFPort.CONTROLLER)
            .build();

            sw.write(po);
          }
          break;

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
        linkDiscoverer = context.getServiceImpl( ILinkDiscoveryService.class );

        Map<String, String> configParameters = context.getConfigParams(this);
        String tmp = configParameters.get("remove-flows-on-link-or-port-down");
        if (tmp != null) {
          REMOVE_FLOWS_ON_LINK_OR_PORT_DOWN = Boolean.parseBoolean(tmp);
        }
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);

    }

    //Watches for link removed events. If the link down comes from s4, increment the
    // s4Counter. If the link comes form s5, increment the s5Counter. If either of these counters
    // is greater than 2, then all the links of that spine switch are down. When this occurs, flow
    // entries are written to the leaf switches to route all outgoing packets to the spine switch that
    // is up.
    public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
        for (LDUpdate u : updateList) {
            /* Remove flows on either side if link/port went down */
            if (u.getOperation().toString() == "Link Removed")
            {
              if (u.getSrc() != null && !u.getSrc().equals(DatapathId.NONE)) {
                IOFSwitch srcSw = switchService.getSwitch(u.getSrc());
                if(srcSw.equals(switches[3])){
                  s4Counter++;
                  if(s4Counter > 2){
                    priority++;
                    for(int i = 0; i < 6; i++){
                      int switchnum = 0; //Switch that flow entries are being made for
                      if(i < 2){
                        switchnum = 0;
                      }
                      else if(i < 4){
                        switchnum = 1;
                      }
                      else if (i < 6){
                        switchnum = 2;
                      }
                      String[] MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:02",
                                              "00:00:00:00:00:03", "00:00:00:00:00:04",
                                              "00:00:00:00:00:05", "00:00:00:00:00:06"};
                      IOFSwitch sw =switchService.getSwitch(switches[(switchnum)]);
                      OFFactory myFactory = sw.getOFFactory();
                      Match match = myFactory.buildMatch()
                      .setExact(MatchField.ETH_SRC, MacAddress.of(MacAddr[i]))
                      .build();

                      ArrayList<OFAction> actionList = new ArrayList<OFAction>();
                      OFActions actions = myFactory.actions();
                      OFActionOutput output = actions.buildOutput()
                      .setMaxLen(0xFFffFFff)
                      .setPort(OFPort.of(2))
                      .build();
                      actionList.add(output);

                      OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                          .setBufferId(OFBufferId.NO_BUFFER)
                          .setPriority(10000 + priority)
                          .setMatch(match)
                          .setActions(actionList)
                          .setTableId(TableId.of(1))
                          .build();

                      sw.write(flowAdd);
                    }
                  }
                }
                if(srcSw.equals(switches[4])){
                  s5Counter++;
                  if(s5Counter > 2){
                    priority++;
                    for(int i = 0; i < 6; i++){
                      int switchnum = 0;
                      if(i < 2){
                        switchnum = 0;
                      }
                      else if(i < 4){
                        switchnum = 1;
                      }
                      else if (i < 6){
                        switchnum = 2;
                      }
                      String[] MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:02",
                                              "00:00:00:00:00:03", "00:00:00:00:00:04",
                                              "00:00:00:00:00:05", "00:00:00:00:00:06"};
                      IOFSwitch sw =switchService.getSwitch(switches[(switchnum)]);
                      OFFactory myFactory = sw.getOFFactory();
                      Match match = myFactory.buildMatch()
                      .setExact(MatchField.ETH_SRC, MacAddress.of(MacAddr[i]))
                      .build();

                      ArrayList<OFAction> actionList = new ArrayList<OFAction>();
                      OFActions actions = myFactory.actions();
                      OFActionOutput output = actions.buildOutput()
                      .setMaxLen(0xFFffFFff)
                      .setPort(OFPort.of(1))
                      .build();
                      actionList.add(output);

                      OFFlowAdd flowAdd = myFactory.buildFlowAdd()
                          .setBufferId(OFBufferId.NO_BUFFER)
                          .setPriority(10000 + priority)
                          .setMatch(match)
                          .setActions(actionList)
                          .setTableId(TableId.of(1))
                          .build();

                      sw.write(flowAdd);
                  }
                }
              }
            }
          }
        }
      }
    }
