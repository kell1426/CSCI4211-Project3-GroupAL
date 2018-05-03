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
    static public HashMap<IPv4Address, MacAddress> ARPmap = new HashMap<>();

    static public DatapathId switches[5];

    public void switchInitializer(DatapathId switches[5]){
      switches[0] = DataPathId.of("00:00:00:00:00:00:00:01");
      switches[1] = DataPathId.of("00:00:00:00:00:00:00:02");
      switches[2] = DataPathId.of("00:00:00:00:00:00:00:03");
      switches[3] = DataPathId.of("00:00:00:00:00:00:00:04");
      switches[4] = DataPathId.of("00:00:00:00:00:00:00:05");
    }

    public void ARPinitilaizer(HashMap<IPv4Address, MacAddress> ARPmap) {
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
    }

    public void FlowTableInitilizer()
    {
      for(int i = 0; i < 5; i++){
        IOFSwitch sw =this.floodlightProvider.getSwitch(switches[i]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        .setExact(MatchField.ETH_TYPE, EthType.of(0x806)) //ARP Type in Hex
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
            // .setHardTimeout(3600)
            // .setIdleTimeout(10)
            .setPriority(32768)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }
      //flow entries from leaves to hosts
      for(int i = 0; i < 6; i++){
        int switchnum;
        if(i < 2){
          switchnum = 0;
        }
        else if(i < 4){
          switchnum = 1;
        }
        else if (i < 6){
          switchnum = 2;
        }
        MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:02",
                                "00:00:00:00:00:03", "00:00:00:00:00:04",
                                "00:00:00:00:00:05", "00:00:00:00:00:06"};
        IOFSwitch sw =this.floodlightProvider.getSwitch(switches[(switchnum)]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        .setExact(MatchField.ETH_DST, EthType.of(MacAddress.of(MacAddr[i]))
        .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFActionOutput output = actions.buildOutput()
        .setMaxLen(0xFFffFFff)
        .setPort(OFPort.of((i % 2) + 3));
        .build();
        actionList.add(output);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
            .setBufferId(OFBufferId.NO_BUFFER)
            // .setHardTimeout(3600)
            // .setIdleTimeout(10)
            .setPriority(32768)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }
      //Flow entries from leaves to spines
      for(int i = 0; i < 6; i++){
        int switchnum;
        if(i < 2){
          switchnum = 0;
        }
        else if(i < 4){
          switchnum = 1;
        }
        else if (i < 6){
          switchnum = 2;
        }
        MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:02",
                                "00:00:00:00:00:03", "00:00:00:00:00:04",
                                "00:00:00:00:00:05", "00:00:00:00:00:06"};
        IOFSwitch sw =this.floodlightProvider.getSwitch(switches[(switchnum)]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        .setExact(MatchField.ETH_SRC, EthType.of(MacAddress.of(MacAddr[i]))
        .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFActionOutput output = actions.buildOutput()
        .setMaxLen(0xFFffFFff)
        .setPort(OFPort.of((i % 2) + 1));
        .build();
        actionList.add(output);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
            .setBufferId(OFBufferId.NO_BUFFER)
            //.setHardTimeout(3600)
            //.setIdleTimeout(10)
            .setPriority(10000)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }
      for(int i = 0; i < 6; i++){
        int switchnum;
        if(i < 3){
          switchnum = 3;
        }
        else if(i < 6){
          switchnum = 4;
        }
        MacAddr = new String[] {"00:00:00:00:00:01", "00:00:00:00:00:03",
                                "00:00:00:00:00:05", "00:00:00:00:00:02",
                                "00:00:00:00:00:04", "00:00:00:00:00:06"};
        IOFSwitch sw =this.floodlightProvider.getSwitch(switches[switchnum]);
        OFFactory myFactory = sw.getOFFactory();
        Match match = myFactory.buildMatch()
        .setExact(MatchField.ETH_DST, EthType.of(MacAddress.of(MacAddr[i])) //ARP Type in Hex
        .build();

        ArrayList<OFAction> actionList = new ArrayList<OFAction>();
        OFActions actions = myFactory.actions();
        OFActionOutput output = actions.buildOutput()
        .setMaxLen(0xFFffFFff)
        .setPort(OFPort.of((i % 3) + 1));
        .build();
        actionList.add(output);

        OFFlowAdd flowAdd = myFactory.buildFlowAdd()
            .setBufferId(OFBufferId.NO_BUFFER)
            // .setHardTimeout(3600)
            // .setIdleTimeout(10)
            .setPriority(32768)
            .setMatch(match)
            .setActions(actionList)
            .setTableId(TableId.of(1))
            .build();

        sw.write(flowAdd);
      }

      OFFactory myFactory = sw.getOFFactory();

      ArrayList<OFGroupMod> groupMods = new ArrayList<OFGroupMod>();
      ArrayList<OFAction> actionList = new ArrayList<OFAction>();

      OFGroupAdd myGroup = myFactory.buildGroupAdd()
        .setGroupType(OFGroupType.ALL)
        .setGroup(OFGroup.of(1))
        .setBuckets(myBucketList)
        .build();

      IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(1));
      mySwitch.write(groupMods);

      ArrayList<OFBucket> bucketList = new ArrayList<OFBucket>();

      OFBucket myBucket = myFactory.buildBucket()
        .setActions(actionList)
        .setWatchGroup(OFGroup.of(23)) /* Monitor liveness of OFGroup 23. */
        .setWatchPort(OFPort.of(7)) /* Monitor liveness of OFPort 7. */
        .build();*/
    }
    //Need to install ARP requests into switches.
    //Go straight from switch to controller.

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
            IPv4Address srcIP = ipv4.getSourceAddress();
            IPv4Address dstIP = ipv4.getDestinationAddress();
            MacAddress srcMac = eth.getSourceMACAddress();
            MacAddress dstMac = ARPmap.get(dstIP);

            Ethernet l2 = new Ethernet();
            l2.setSourceMACAddress(dstMac);
            l2.setDestinationMACAddress(srcMac);
            l2.setEtherType(EthType.ARP);

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

            OFPacketOut po = sw.getOFFactory().buildPacketOut() /* mySwitch is some IOFSwitch object */
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
    }

    @Override
    public void startUp(FloodlightModuleContext context) {
        floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
    }

    // @Override
    // public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
    //     for (LDUpdate u : updateList) {
    //         /* Remove flows on either side if link/port went down */
    //         if (u.getOperation() == UpdateOperation.LINK_REMOVED ||
    //                 u.getOperation() == UpdateOperation.PORT_DOWN ||
    //                 u.getOperation() == UpdateOperation.TUNNEL_PORT_REMOVED) {
    //             Set<OFMessage> msgs = new HashSet<OFMessage>();
    //
    //             if (u.getSrc() != null && !u.getSrc().equals(DatapathId.NONE)) {
    //                 IOFSwitch srcSw = switchService.getSwitch(u.getSrc());
    //                 /* src side of link */
    //                 if (srcSw != null) {
    //                     Set<U64> ids = flowSetIdRegistry.getFlowSetIds(
    //                             new NodePortTuple(u.getSrc(), u.getSrcPort()));
    //                     if (ids != null) {
    //                         Iterator<U64> i = ids.iterator();
    //                         while (i.hasNext()) {
    //                             U64 id = i.next();
    //                             U64 cookie = id.or(DEFAULT_FORWARDING_COOKIE);
    //                             U64 cookieMask = U64.of(FLOWSET_MASK).or(AppCookie.getAppFieldMask());
    //
    //                             /* Delete flows matching on src port and outputting to src port */
    //                             msgs = buildDeleteFlows(u.getSrcPort(), msgs, srcSw, cookie, cookieMask);
    //                             messageDamper.write(srcSw, msgs);
    //                             log.debug("src: Removing flows to/from DPID={}, port={}", u.getSrc(), u.getSrcPort());
    //                             log.debug("src: Cookie/mask {}/{}", cookie, cookieMask);
    //
    //                             /*
    //                              * Now, for each ID on this particular failed link, remove
    //                              * all other flows in the network using this ID.
    //                              */
    //                             Set<NodePortTuple> npts = flowSetIdRegistry.getNodePortTuples(id);
    //                             if (npts != null) {
    //                                 for (NodePortTuple npt : npts) {
    //                                     msgs.clear();
    //                                     IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
    //                                     if (sw != null) {
    //
    //                                         /* Delete flows matching on npt port and outputting to npt port*/
    //                                         msgs = buildDeleteFlows(npt.getPortId(), msgs, sw, cookie, cookieMask);
    //                                         messageDamper.write(sw, msgs);
    //                                         log.debug("src: Removing same-cookie flows to/from DPID={}, port={}", npt.getNodeId(), npt.getPortId());
    //                                         log.debug("src: Cookie/mask {}/{}", cookie, cookieMask);
    //                                     }
    //                                 }
    //                             }
    //                             flowSetIdRegistry.removeExpiredFlowSetId(id, new NodePortTuple(u.getSrc(), u.getSrcPort()), i);
    //                         }
    //                     }
    //                 }
    //                 flowSetIdRegistry.removeNodePortTuple(new NodePortTuple(u.getSrc(), u.getSrcPort()));
    //             }
    //
    //             /* must be a link, not just a port down, if we have a dst switch */
    //             if (u.getDst() != null && !u.getDst().equals(DatapathId.NONE)) {
    //                 /* dst side of link */
    //                 IOFSwitch dstSw = switchService.getSwitch(u.getDst());
    //                 if (dstSw != null) {
    //                     Set<U64> ids = flowSetIdRegistry.getFlowSetIds(
    //                             new NodePortTuple(u.getDst(), u.getDstPort()));
    //                     if (ids != null) {
    //                         Iterator<U64> i = ids.iterator();
    //                         while (i.hasNext()) {
    //                             U64 id = i.next();
    //                             U64 cookie = id.or(DEFAULT_FORWARDING_COOKIE);
    //                             U64 cookieMask = U64.of(FLOWSET_MASK).or(AppCookie.getAppFieldMask());
    //                             /* Delete flows matching on dst port and outputting to dst port */
    //                             msgs = buildDeleteFlows(u.getDstPort(), msgs, dstSw, cookie, cookieMask);
    //                             messageDamper.write(dstSw, msgs);
    //                             log.debug("dst: Removing flows to/from DPID={}, port={}", u.getDst(), u.getDstPort());
    //                             log.debug("dst: Cookie/mask {}/{}", cookie, cookieMask);
    //
    //                             /*
    //                              * Now, for each ID on this particular failed link, remove
    //                              * all other flows in the network using this ID.
    //                              */
    //                             Set<NodePortTuple> npts = flowSetIdRegistry.getNodePortTuples(id);
    //                             if (npts != null) {
    //                                 for (NodePortTuple npt : npts) {
    //                                     msgs.clear();
    //                                     IOFSwitch sw = switchService.getSwitch(npt.getNodeId());
    //                                     if (sw != null) {
    //                                         /* Delete flows matching on npt port and outputting on npt port */
    //                                         msgs = buildDeleteFlows(npt.getPortId(), msgs, sw, cookie, cookieMask);
    //                                         messageDamper.write(sw, msgs);
    //                                         log.debug("dst: Removing same-cookie flows to/from DPID={}, port={}", npt.getNodeId(), npt.getPortId());
    //                                         log.debug("dst: Cookie/mask {}/{}", cookie, cookieMask);
    //                                     }
    //                                 }
    //                             }
    //                             flowSetIdRegistry.removeExpiredFlowSetId(id, new NodePortTuple(u.getDst(), u.getDstPort()), i);
    //                         }
    //                     }
    //                 }
    //                 flowSetIdRegistry.removeNodePortTuple(new NodePortTuple(u.getDst(), u.getDstPort()));
    //             }
    //         }
    //     }
    // }

}





/*case ARP IN:

get src port
get src Mac
get src IP
get dst IP

convert dst IP to Mac

create ARP response with dst Mac

write to switch*/
