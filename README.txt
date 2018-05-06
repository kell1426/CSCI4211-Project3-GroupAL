CSCI 4211 Project 4
Group AL
Daniel Kelly
Noah Bolduan

Code implementation and design details:
On startup of the controller and the initialization of the switches and hosts, the controller will write flow entries to each switch. First, a flow entry forcing all ARP packets to be sent straight to the controller is written to each switch. This prevents flooding ARP request packets when the hosts try to ping each other. Second, all the routing flow entries are written to each corresponding switch. Flow entries that match on destination MAC addresses are given a higher priority so that the packet will be sent down to the host rather than forwarding out to the network. Entries that match on sources have a lower priority and will send the packet either to S4 or S5 depending on the source host (h1, h3, and h5 go to S4. h2, h4, and h6 go to S5).

When all the links of a spine switch go down, the module will accommodate for this. A link discovery service listener is in place that will watch for link down events. When all 3 links of a spine switch go down, new entries are written to the leaf switches that have a higher priority than the original entries that forward packets to the spine switch. These new entries route the packets to the running spine switch, and the original rules in this switch will correctly send out packets to the leaf switches.

Running the Project:
To run the code, move to the mininet/floodlight directory and run the command "ant". This will build the target module. Then run the command "java -jar target/floodlight.jar". This will run the floodlight module and create the routing network. In another terminal of mininet, run the command "sudo mn --controller=remote,ip=127.0.0.1,port=6653 --custom topology.py --topo mytopo --mac". This will setup the mininet network. At this point, mininet command can be run to test the network.

Individual Contributions
The code in this program was worked on equally between Daniel and Noah. Functions and methods were written together and both of us did necessary research on how to work with floodlight.

Some smaller individual tasks that were done individually:
Noah: Designed the data structures used to map IP addresses to MAC addresses.
Dan: Ran the floodlight and mininet programs to work on debugging.
