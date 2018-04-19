from mininet.cli import CLI
from mininet.net import Mininet
from mininet.link import TCLink
from mininet.topo import Topo
from mininet.log import setLogLevel
from mininet.node import RemoteController

class AssignmentNetworks(Topo):
    def __init(self, **opts):
        Topo.__init__(self, **opts)
        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        h3 = self.addHost('h3')
        h4 = self.addHost('h4')
        h5 = self.addHost('h5')
        h6 = self.addHost('h6')
        h7 = self.addHost('h7')
        h8 = self.addHost('h8')
        c1 = self.addSwitch('c1')
        a1 = self.addSwitch('a1')
        a2 = self.addSwitch('a2')
        e1 = self.addSwitch('e1')
        e2 = self.addSwitch('e2')
        e3 = self.addSwitch('e3')
        e4 = self.addSwitch('e4')

        self.addLink(h1, e1)
        self.addLink(h2, e1)
        self.addLink(h3, e2)
        self.addLink(h4, e2)
        self.addLink(h5, e3)
        self.addLink(h6, e3)
        self.addLink(h7, e4)
        self.addLink(h8, e4)

        self.addLink(c1, a1, bw=40, delay = '20ms')
        self.addLink(c1, a2, bw=40, delay = '20ms')
        self.addLink(a1, e1, bw=20, delay = '20ms')
        self.addLink(a1, e2, bw=20, delay = '20ms')
        self.addLink(a2, e3, bw=20, delay = '20ms')
        self.addLink(a2, e4, bw=20, delay = '20ms')


if __name__ == '__main__':
    setLogLevel( 'info' )

    # Create data network
    topo = AssignmentNetworks()
    net = Mininet(controller=RemoteController ,topo=topo, link=TCLink, autoSetMacs=True,
           autoStaticArp=True)

    # Run network
    net.start()
    CLI( net )
    net.stop()
