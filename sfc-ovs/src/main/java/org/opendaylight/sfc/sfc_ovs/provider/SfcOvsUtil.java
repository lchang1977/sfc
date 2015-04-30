/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/**
 * SfcOvsUtil class contains various wrapper and utility methods
 * <p/>
 * <p/>
 *
 * @author Andrej Kincel (andrej.kincel@gmail.com)
 * @version 0.1
 * @since 2015-04-01
 */

package org.opendaylight.sfc.sfc_ovs.provider;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcOvsDataStoreAPI;
import org.opendaylight.sfc.sfc_ovs.provider.api.SfcSffToOvsMappingAPI;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.service.function.forwarder.SffDataPlaneLocator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SfcOvsUtil {

    private static final Logger LOG = LoggerFactory.getLogger(SfcOvsUtil.class);
    private static final String OVSDB_BRIDGE_PREFIX = "/bridge/";
    private static final String OVSDB_TERMINATION_POINT_PREFIX = "/terminationpoint/";
    public static final String OVSDB_OPTION_LOCAL_IP = "local_ip";
    public static final String OVSDB_OPTION_REMOTE_IP = "remote_ip";
    public static final String OVSDB_OPTION_DST_PORT = "dst_port";
    public static final String OVSDB_OPTION_NSP = "nsp";
    public static final String OVSDB_OPTION_NSI = "nsi";
    public static final String OVSDB_OPTION_KEY = "key";
    public static final PortNumber NSH_VXLAN_TUNNEL_PORT = new PortNumber(6633);

    /**
     * Submits callable for execution by given ExecutorService.
     * Thanks to this wrapper method, boolean result will be returned instead of Future.
     * <p/>
     *
     * @param callable Callable
     * @param executor ExecutorService
     * @return true if callable completed successfully, otherwise false.
     */
    public static Object submitCallable(Callable callable, ExecutorService executor) {
        Future future = null;
        Object result = null;

        future = executor.submit(callable);

        try {
            result = future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("{} failed to: {}", callable.toString(), e);
        }

        return result;
    }

    /**
     * Method builds OVSDB Topology InstanceIdentifier
     * <p/>
     *
     * @return InstanceIdentifier<Topology>
     */
    public static InstanceIdentifier<Topology> buildOvsdbTopologyIID() {
        InstanceIdentifier<Topology> ovsdbTopologyIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID));

        return ovsdbTopologyIID;
    }

    /**
     * Method builds OVS NodeId which is based on:
     * 1. OVS Node InstanceIdentifier which manages the OVS Bridge
     * 2. OVS Bridge name
     * <p/>
     * If the two aforementioned fields are missing, NullPointerException is raised.
     * <p/>
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return NodeId
     */
    private static NodeId getManagedByNodeId(OvsdbBridgeAugmentation ovsdbBridge) {
        Preconditions.checkNotNull(ovsdbBridge, "Cannot getManagedByNodeId, OvsdbBridgeAugmentation is null.");

        Preconditions.checkNotNull(ovsdbBridge.getBridgeName(), "Cannot build getManagedByNodeId, BridgeName is null.");
        Preconditions.checkNotNull(ovsdbBridge.getManagedBy(), "Cannot build getManagedByNodeId, ManagedBy is null.");
        String bridgeName = (ovsdbBridge.getBridgeName().getValue());
        InstanceIdentifier<Node> nodeIID = (InstanceIdentifier<Node>) ovsdbBridge.getManagedBy().getValue();

        KeyedInstanceIdentifier keyedInstanceIdentifier = (KeyedInstanceIdentifier) nodeIID.firstIdentifierOf(Node.class);
        Preconditions.checkNotNull(keyedInstanceIdentifier, "Cannot build getManagedByNodeId, parent OVS Node is null.");

        NodeKey nodeKey = (NodeKey) keyedInstanceIdentifier.getKey();
        String nodeId = nodeKey.getNodeId().getValue();
        nodeId = nodeId.concat(OVSDB_BRIDGE_PREFIX + bridgeName);

        return new NodeId(nodeId);
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on OVS NodeId
     * <p/>
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return InstanceIdentifier<Node>
     * @see SfcOvsUtil getManagedByNodeId
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(OvsdbBridgeAugmentation ovsdbBridge) {
        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(getManagedByNodeId(ovsdbBridge)));

        return nodeIID;
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on Service Function Forwarder name.
     * Method will return valid InstanceIdentifier only if the given SFF name belongs to SFF instance mapped to OVS.
     * <p/>
     *
     * @param serviceFunctionForwarderName String
     * @return InstanceIdentifier<Node>
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(String serviceFunctionForwarderName) {
        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(new NodeId(serviceFunctionForwarderName)));

        return nodeIID;
    }

    /**
     * Method builds OVS Node InstanceIdentifier which is based on NodeId.
     * <p/>
     *
     * @param nodeId NodeId
     * @return InstanceIdentifier<Node>
     */
    public static InstanceIdentifier<Node> buildOvsdbNodeIID(NodeId nodeId) {
        InstanceIdentifier<Node> nodeIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(nodeId));

        return nodeIID;
    }

    /**
     * Method builds OVS BridgeAugmentation InstanceIdentifier which is based on OVS NodeId
     * <p/>
     *
     * @param ovsdbBridge OvsdbBridgeAugmentation
     * @return InstanceIdentifier<OvsdbBridgeAugmentation>
     * @see SfcOvsUtil getManagedByNodeId
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(OvsdbBridgeAugmentation ovsdbBridge) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(getManagedByNodeId(ovsdbBridge)))
                        .augmentation(OvsdbBridgeAugmentation.class);

        return bridgeEntryIID;
    }

    /**
     * Method builds OVS BridgeAugmentation InstanceIdentifier which is based on OVS Bridge name
     * <p/>
     *
     * @param serviceFunctionForwarderName serviceFunctionForwarderName String
     * @return InstanceIdentifier<OvsdbBridgeAugmentation>
     */
    public static InstanceIdentifier<OvsdbBridgeAugmentation> buildOvsdbBridgeIID(String serviceFunctionForwarderName) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeEntryIID = buildOvsdbNodeIID(serviceFunctionForwarderName)
                .augmentation(OvsdbBridgeAugmentation.class);

        return bridgeEntryIID;
    }

    /**
     * Method builds OVS TerminationPointAugmentation InstanceIdentifier which is based on:
     * 1. OVS Node InstanceIdentifier which manages the OVS Bridge, to which the OVS TerminationPoint is attached
     * 2. OVS Termination Point name
     * <p/>
     * If the two aforementioned fields are missing, NullPointerException is raised.
     * <p/>
     *
     * @param ovsdbBridge           OvsdbBridgeAugmentation
     * @param ovsdbTerminationPoint OvsdbTerminationPointAugmentation
     * @return InstanceIdentifier<OvsdbTerminationPointAugmentation>
     */
    public static InstanceIdentifier<OvsdbTerminationPointAugmentation> buildOvsdbTerminationPointAugmentationIID(
            OvsdbBridgeAugmentation ovsdbBridge, OvsdbTerminationPointAugmentation ovsdbTerminationPoint) {

        Preconditions.checkNotNull(ovsdbTerminationPoint,
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier, OvsdbTerminationPointAugmentation is null.");
        Preconditions.checkNotNull(ovsdbTerminationPoint.getName(),
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier, OvsdbTerminationPointAugmentation Name is null.");
        Preconditions.checkNotNull(ovsdbBridge,
                "Cannot build OvsdbTerminationPointAugmentation InstanceIdentifier, OvsdbBridgeAugmentation is null.");

        NodeId nodeId = getManagedByNodeId(ovsdbBridge);
        String terminationPointId = nodeId.getValue() + OVSDB_TERMINATION_POINT_PREFIX + ovsdbTerminationPoint.getName();

        InstanceIdentifier<OvsdbTerminationPointAugmentation> terminationPointIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(nodeId))
                        .child(TerminationPoint.class, new TerminationPointKey(new TpId(terminationPointId)))
                        .augmentation(OvsdbTerminationPointAugmentation.class);

        return terminationPointIID;
    }

    /**
     * Method builds OVS TerminationPoint InstanceIdentifier which is based on SFF name and SFF DataPlane locator name.
     * Method will return valid InstanceIdentifier only if the given SFF and SFF DataPlane locator
     * belongs to SFF instance mapped to OVS.
     * <p/>
     *
     * @param sffName                 Service Function Forwarder Name
     * @param sffDataPlaneLocatorName Service Function Forwarder Data Plane locator name
     * @return InstanceIdentifier<TerminationPoint>
     */
    public static InstanceIdentifier<TerminationPoint> buildOvsdbTerminationPointIID(String sffName, String sffDataPlaneLocatorName) {
        InstanceIdentifier<TerminationPoint> terminationPointIID =
                InstanceIdentifier
                        .create(NetworkTopology.class)
                        .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                        .child(Node.class, new NodeKey(new NodeId(sffName)))
                        .child(TerminationPoint.class, new TerminationPointKey(
                                new TpId(sffName + OVSDB_TERMINATION_POINT_PREFIX + sffDataPlaneLocatorName)));

        return terminationPointIID;
    }

    public static IpAddress convertStringToIpAddress(String ipAddressString) {
        Preconditions.checkNotNull(ipAddressString, "Supplied string value of ipAddress must not be null");

        try {
            return new IpAddress(new Ipv4Address(ipAddressString));
        } catch (Exception e) {
            LOG.debug("Supplied string value of ipAddress ({}) is not an instance of IPv4", ipAddressString);
        }

        try {
            return new IpAddress(new Ipv6Address(ipAddressString));
        } catch (Exception e) {
            LOG.debug("Supplied string value of ipAddress ({}) is not an instance of IPv6", ipAddressString);
        }

        LOG.error("Supplied string value of ipAddress ({}) cannot be converted to IpAddress object!", ipAddressString);
        return null;
    }

    public static String convertIpAddressToString(IpAddress ipAddress) {
        Preconditions.checkNotNull(ipAddress, "Supplied IpAddress value must not be null");

        try {
            Preconditions.checkArgument(ipAddress.getIpv4Address().getValue() != null);
            return ipAddress.getIpv4Address().getValue();
        } catch (Exception e) {
            LOG.debug("Supplied IpAddress value ({}) is not an instance of IPv4", ipAddress.toString());
        }

        try {
            Preconditions.checkArgument(ipAddress.getIpv6Address().getValue() != null);
            return ipAddress.getIpv6Address().getValue();
        } catch (Exception e) {
            LOG.debug("Supplied IpAddress value ({}) is not an instance of IPv6", ipAddress.toString());
        }

        LOG.error("Supplied IpAddress value ({}) cannot be converted to String!", ipAddress.toString());
        return null;
    }

    public static boolean putOvsdbTerminationPoints(OvsdbBridgeAugmentation ovsdbBridge,
                                                    List<SffDataPlaneLocator> sffDataPlaneLocatorList,
                                                    ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        boolean result = true;
        List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList =
                SfcSffToOvsMappingAPI.buildTerminationPointAugmentationList(sffDataPlaneLocatorList);

        for (OvsdbTerminationPointAugmentation ovsdbTerminationPoint : ovsdbTerminationPointList) {
            Object[] methodParameters = {ovsdbBridge, ovsdbTerminationPoint};
            SfcOvsDataStoreAPI sfcOvsDataStoreAPIPutTerminationPoint =
                    new SfcOvsDataStoreAPI(
                            SfcOvsDataStoreAPI.Method.PUT_OVSDB_TERMINATION_POINT,
                            methodParameters
                    );
            boolean partialResult =
                    (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIPutTerminationPoint, executor);

            //once result is false, we will keep it false (it will be not overwritten with next partialResults)
            if (result) {
                result = partialResult;
            }
        }

        return result;
    }

    public static boolean putOvsdbBridge(OvsdbBridgeAugmentation ovsdbBridge, ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        Object[] methodParameters = {ovsdbBridge};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIPutBridge =
                new SfcOvsDataStoreAPI(
                        SfcOvsDataStoreAPI.Method.PUT_OVSDB_BRIDGE,
                        methodParameters
                );
        return (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIPutBridge, executor);
    }

    public static boolean deleteOvsdbNode(InstanceIdentifier<Node> ovsdbNodeIID, ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        Object[] methodParameters = {ovsdbNodeIID};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIDeleteNode = new SfcOvsDataStoreAPI(
                SfcOvsDataStoreAPI.Method.DELETE_OVSDB_NODE,
                methodParameters
        );
        return (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIDeleteNode, executor);
    }

    public static boolean deleteOvsdbTerminationPoint(InstanceIdentifier<TerminationPoint> ovsdbTerminationPointIID,
                                                      ExecutorService executor) {
        Preconditions.checkNotNull(executor);

        Object[] methodParameters = {ovsdbTerminationPointIID};
        SfcOvsDataStoreAPI sfcOvsDataStoreAPIDeleteTerminationPoint = new SfcOvsDataStoreAPI(
                SfcOvsDataStoreAPI.Method.DELETE_OVSDB_TERMINATION_POINT,
                methodParameters
        );
        return (boolean) SfcOvsUtil.submitCallable(sfcOvsDataStoreAPIDeleteTerminationPoint, executor);
    }
}