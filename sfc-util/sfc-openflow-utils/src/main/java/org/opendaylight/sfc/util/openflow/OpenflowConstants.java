/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.util.openflow;

public final class OpenflowConstants {

    // identifies initialization flows installed in the SFFs - since they don't belong
    // to a particular NSP / RSP path ID
    public static final long SFC_FLOWS = 0xdeadbeef;

    // Ethernet NextProtocol/EtherType for NSH
    public static final long ETHERTYPE_NSH = 0x894F;

    // Packet Type constants
    public static final int PACKET_TYPE_ETH = 0;
    public static final int PACKET_TYPE_NSH = 0x1894f;

    // NSH constants
    public static final short NSH_MDTYPE_1 = 1;

    // OpenFlow table names and related:
    //   used by both OpenflowRenderer and SFC statistics
    public static final String OF_NAME_DELIMITER = "_";
    public static final String OF_NAME_MATCH_ANY = "matchAny";
    public static final String OF_NAME_MATCH_ANY_DROP = "matchAnyDrop";
    public static final String OF_NAME_MATCH_ANY_RESUBMIT = "matchAnyResubmit";
    public static final String OF_NAME_CLASS_DPDK_OUT = "classifierDpdkOutput";
    public static final String OF_NAME_CLASS_DPDK_IN = "classifierDpdkInput";
    public static final String OF_NAME_TRANSPORT_INGRESS = "transportIngress";
    public static final String OF_NAME_TRANSPORT_INGRESS_NSH = "transportIngressNsh";
    public static final String OF_NAME_TRANSPORT_INGRESS_ETH_NSH = "transportIngressEthNsh";
    public static final String OF_NAME_TRANSPORT_INGRESS_ARP = "transportIngressArp";
    public static final String OF_NAME_TRANSPORT_INGRESS_MAC = "transportIngressMac";
    public static final String OF_NAME_TRANSPORT_INGRESS_MPLS = "transportIngressMpls";
    public static final String OF_NAME_TRANSPORT_INGRESS_TCP = "transportIngressTcp";
    public static final String OF_NAME_TRANSPORT_INGRESS_UDP = "transportIngressUdp";
    public static final String OF_NAME_TRANSPORT_INGRESS_VLAN = "transportIngressVlan";
    public static final String OF_NAME_PATH_MAPPER = "pathMapper";
    public static final String OF_NAME_PATH_MAPPER_ACL = "pathMapperAcl";
    public static final String OF_NAME_NEXT_HOP = "nextHop";
    public static final String OF_NAME_TRANSPORT_EGRESS = "transportEgress";
    public static final String OF_NAME_LASTHOP_TRANSPORT_EGRESS = "transportEgressLastHop";
    public static final String OF_NAME_LASTHOP_TRANSPORT_EGRESS_PIPELINE = "transportEgressLastHop_Pipeline";
    public static final String OF_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_REMOTE = "transportEgressLastHop_TunnelRemote";
    public static final String OF_NAME_LASTHOP_TRANSPORT_EGRESS_TUNNEL_LOCAL = "transportEgressLastHop_TunnelLocal";
    public static final String OF_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_REMOTE = "transportEgressLastHop_NshRemote";
    public static final String OF_NAME_LASTHOP_TRANSPORT_EGRESS_NSH_LOCAL = "transportEgressLastHop_NshLocal";
    public static final String OF_NAME_APPCOEXIST_TRANSPORT_EGRESS = "transportEgressAppCoexist";
    public static final String OF_NAME_SF_LOOPBACK_INGRESS = "sfLoopbackIngress";
    public static final String OF_NAME_SF_LOOPBACK_EGRESS = "sfLoopbackEgress";

    private OpenflowConstants() {
    }
}
