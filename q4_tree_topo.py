#!/usr/bin/env python

from mininet.cli import CLI
from mininet.net import Mininet
from mininet.link import TCLink
from mininet.topo import Topo
from mininet.log import setLogLevel
#from pox import POX

class AssignmentNetworks(Topo):
    def __init__(self, **opts):
        Topo.__init__(self, **opts)
        lvl1_bw = 100
        lvl2_bw = 40
        lvl3_bw = 10

        lvl1_delay = '30ms'
        lvl2_delay = '20ms'
        lvl3_delay = '10ms'

        #PROJ3 Start to build the tree here.

        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        h3 = self.addHost('h3')
        h4 = self.addHost('h4')
        h5 = self.addHost('h5')
        h6 = self.addHost('h6')
        h7 = self.addHost('h7')
        h8 = self.addHost('h8')
        s1 = self.addSwitch('s1') #c1
        s2 = self.addSwitch('s2') #a1
        s3 = self.addSwitch('s3') #a2
        s4 = self.addSwitch('s4') #e1
        s5 = self.addSwitch('s5') #e2
        s6 = self.addSwitch('s6') #e3
        s7 = self.addSwitch('s7') #e4

        self.addLink(h1, s4, bw=lvl3_bw, delay=lvl3_delay)
        self.addLink(h2, s4, bw=lvl3_bw, delay=lvl3_delay)
        self.addLink(h3, s5, bw=lvl3_bw, delay=lvl3_delay)
        self.addLink(h4, s5, bw=lvl3_bw, delay=lvl3_delay)
        self.addLink(h5, s6, bw=lvl3_bw, delay=lvl3_delay)
        self.addLink(h6, s6, bw=lvl3_bw, delay=lvl3_delay)
        self.addLink(h7, s7, bw=lvl3_bw, delay=lvl3_delay)
        self.addLink(h8, s7, bw=lvl3_bw, delay=lvl3_delay)

        self.addLink(s3, s6, bw=lvl2_bw, delay=lvl2_delay)
        self.addLink(s3, s7, bw=lvl2_bw, delay=lvl2_delay)
        self.addLink(s2, s4, bw=lvl2_bw, delay=lvl2_delay)
        self.addLink(s2, s5, bw=lvl2_bw, delay=lvl2_delay)
        self.addLink(s1, s2, bw=lvl1_bw, delay=lvl1_delay)
        self.addLink(s1, s3, bw=lvl1_bw, delay=lvl1_delay)



if __name__ == '__main__':
    setLogLevel( 'info' )

    topo = AssignmentNetworks()
    net = Mininet(topo=topo, link=TCLink, autoSetMacs=True,
           autoStaticArp=True)

    # Run network
    net.start()
    CLI( net )
    net.stop()
