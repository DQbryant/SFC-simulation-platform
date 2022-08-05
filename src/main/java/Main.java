import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;

import components.*;

import migration.DataCollector;
import migration.PolicyImplementer;
import placement.NetworkInspector;
import placement.PlacementMaker;

import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static NetworkInspector networkInspector = new NetworkInspector();
    public static void main(String[] args) throws InterruptedException, FileNotFoundException {
        Scanner scanner = new Scanner(System.in);
        int action;
        char flag;
        Topo topo = readTopoStorage();
        networkInspector = new NetworkInspector();
        networkInspector.setTopo(topo);
        List<SFC> sfcs = null;
        PlacementMaker placementMaker = new PlacementMaker();

        // �־û��������˺�����

        File file = new File("requests.json");
        if(file.exists()){
            String jsonString = FileUtil.readString(new File("requests.json"), Charset.forName("GBK"));
            sfcs = JSONUtil.parseArray(jsonString).toList(SFC.class);
            topo.setSfcs(sfcs);
        }
//        if(topo != null){
//            topo.setVnfs(readVNFs(sfcs, topo));
//        }
        if(sfcs == null){
            sfcs = new ArrayList<>();
        }
        boolean isDeployed = false;
        List<SFC> sfcleft = new ArrayList<>();
        int index = sfcs.size() + 1;

        while(true){
            System.out.println("����������Ҫִ�еĲ��������:");
            System.out.println("1. ��������");
            System.out.println("2. ����SFC����");
            System.out.println("3. ����SFC����");
            System.out.println("4. �޸�SFC����������");
            System.out.println("5. ִ��������");
            System.out.println("6. ִ��Ǩ���㷨");
            System.out.println("7. ���۵�ǰSFC���𷽰�");
            System.out.println("8. ���۵�ǰ������Դռ�����");
            System.out.println("9. ��ӡ��ǰ����״��");
            System.out.println("10. ��ӡSFC�������");
            System.out.println("11. �˳�(ע��!�Ƴ�������������Ϣ���ᱻ����!)");
            action = scanner.nextInt();
            switch(action) {
                case 1: {
                    if (topo != null){
                        System.out.println("��ǰ�Ѿ�������������,�Ƿ񸲸�?(y/n):");
                        flag = scanner.next().charAt(0);
                        if(flag == 'y') {
                            topo = inputTopology();
                            networkInspector.setTopo(topo);
                            topo.setSfcs(sfcs);
                        }
                    }else {
                        topo = inputTopology();
                        networkInspector.setTopo(topo);
                        topo.setSfcs(sfcs);
                    }
                    break;
                }
                case 2:{
                    if(topo == null){
                        System.out.println("���ȴ���������Ϣ!");
                        break;
                    }
                    if (sfcs.size() != 0){
                        System.out.println("��ǰ�Ѿ�����SFC���󼯺�,�Ƿ񸲸�?(y/n):");
                        flag = scanner.next().charAt(0);
                        if(flag == 'y') {
                            sfcs = inputRequest();
                            topo.setSfcs(sfcs);
                            sfcleft.clear();
                        }
                    }else {
                        sfcs = inputRequest();
                        topo.setSfcs(sfcs);
                        sfcleft.clear();
                    }
                    index = sfcs.size()+1;
                    break;
                }
                case 3:{
                    printSFCRequestInfo();
                    sfcleft.add(inputSFC(index++));
                    if(!isDeployed){
                        sfcs.addAll(sfcleft);
                        sfcleft.clear();
                    }
                    storeSFC(sfcs, sfcleft);
                    break;
                }
                case 5:{
                    if(sfcs.size()==0 || topo == null){
                        System.out.println("���ȴ������˺͹���sfc����");
                    }else {
                        if(topo.getVnfs()!= null && topo.getVnfs().size() != 0){
                            System.out.println("��ǰ�Ѿ����ڲ�����Ϣ,�Ƿ�����?(y/n)");
                            flag = scanner.next().charAt(0);
                            if(flag == 'y') {
                                sfcs.forEach(sfc -> {
                                    sfc.setAccessToVNF0(null);
                                    sfc.setVnfList(null);
                                    sfc.setLinkList(null);
                                });
                                topo = readTopoStorage();
                                topo.setSfcs(sfcs);
                                networkInspector.setTopo(topo);
                                isDeployed = false;
                                NetworkInspector.index = 1;
                            }else {
                                break;
                            }
                        }
                        System.out.println("��ѡ����Ҫִ�еķ����㷨�����");
                        System.out.println("1. �����Ŵ��㷨��SFC�����㷨");
                        System.out.println("2. ����̰�ĵ�SFC�����㷨");
                        int algorithmNum = scanner.nextInt();
                        if(isDeployed){
                            if(sfcleft.size() != 0){
                                topo.getVnfs().addAll(placementMaker.makePlacement(topo, sfcleft, algorithmNum));
                                topo.getSfcs().addAll(sfcleft);
                                sfcleft.clear();
//                                storeVNFs(topo.getVnfs(), sfcs, topo);
                            }else {
                                System.out.println("��ǰ�����ڻ�δ�����SFC����!");
                            }
                        }else {
                            topo.setVnfs(placementMaker.makePlacement(topo, topo.getSfcs(), algorithmNum));
                            isDeployed = true;
//                            storeVNFs(topo.getVnfs(), sfcs, topo);
                        }
                    }
                    break;
                }
                case 4:{
                    if(topo == null){
                        System.out.println("���ȴ���������Ϣ!");
                        break;
                    }
                    changeState(topo, index, isDeployed);
                    break;
                }
                case 6:{
                    if(topo == null || topo.getVnfs() == null || topo.getVnfs().size() == 0){
                        System.out.println("���ȴ���������Ϣ��ִ�в���");
                        break;
                    }
                    System.out.println("��ѡ����Ҫִ�е�Ǩ��(����)�㷨�����");
                    System.out.println("1. ��׼��̬VNFǨ���㷨");
                    System.out.println("2. ���Ż���VNF����Ǩ���㷨");
                    System.out.println("3. ���ؾ����VNF����Ǩ���㷨");
                    System.out.println("4. ��ͳVNFǨ���㷨");
                    int algorithmNum = scanner.nextInt();
                    PolicyImplementer policyImplementer = new PolicyImplementer(new DataCollector(topo));
                    policyImplementer.adjust(algorithmNum);
//                    storeVNFs(topo.getVnfs(), sfcs, topo);
                    break;
                }
                case 7:{
                    if (topo == null || sfcs.size() == 0 || topo.getVnfs().size() == 0){
                        System.out.println("���ȴ������˺͹���sfc����ִ�в���");
                        break;
                    }
                    System.out.println("���۽������:");
                    networkInspector.evaluateSFC();
                    break;
                }
                case 8:{
                    if(topo == null){
                        System.out.println("���ȴ���������Ϣ!");
                        break;
                    }
                    System.out.println("���۽������:");
                    networkInspector.evaluateTopo();
                    break;
                }
                case 9: {
                    if(topo == null){
                        System.out.println("��������������Ϣ��");
                        break;
                    }
                    System.out.println("��ǰ����״̬����:");
                    networkInspector.printNetworkInfo();
                    break;
                }
                case 10: {
                    if(sfcs.size() == 0){
                        System.out.println("��������SFC����");
                        break;
                    }
                    System.out.println("SFC����״������:");
                    networkInspector.printSFCRequestInfo();
                    break;
                }
                case 99: {
                    if (topo != null){
                        topo.setVnfs(inputVNF(topo));
                        isDeployed = true;
                    }
                }
            }

            if(action == 12)break;
        }

    }
    public static Topo inputTopology() throws InterruptedException, FileNotFoundException {

        System.out.println("��ѡ�������������˵ķ�ʽ:");
        Thread.sleep(500);
        System.out.println("1. ����������");
        System.out.println("2. �ļ�����(��ȷ��topology.txt���ļ�Ŀ¼���Ҹ�ʽ��ȷ!)");
        Scanner scanner = new Scanner(System.in);
        int inputWay = scanner.nextInt();
        System.out.println("�������Ƿ����P4������?(y/n)");
        char c = scanner.next().charAt(0);
        if(inputWay == 2){
            File file = new File("topology.txt");
            if(file.exists()){
                scanner = new Scanner(new FileInputStream(file));
            }
        }

        if(inputWay == 1)System.out.println("������ڵ�����:");
        /**
         * ������ʽ����һ�нڵ�����������ÿ���ڵ����Դ������ͬ��
         */
        int nodeNum = scanner.nextInt();
        List<Node> nodes = new ArrayList<>();
        int flag;
        if(c == 'y'){
            if(inputWay == 1)System.out.println("��������ÿ���ڵ��Ƿ���P4������:1��ʾ��, 0��ʾ����");
            for (int i = 1; i <= nodeNum; i++) {
                if(inputWay == 1)System.out.print("��ǰ�ڵ�"+i+":");
                flag = scanner.nextInt();
                nodes.add(new Node(i, Constant.nodeCpuResource / (flag == 1 ? 2 : 1), Constant.nodeMemResource / (flag == 1 ? 2 : 1), flag == 1));
            }
        }else {
            for (int i = 1; i <= nodeNum; i++) {
                nodes.add(new Node(i, Constant.nodeCpuResource, Constant.nodeMemResource));
            }
        }

        /**
         * ��������·������
         * ����Ҫָ����·�Ĵ���,������Ҫָ����·���ӵĽڵ���������
         * ����:1 2 ��ʾ1��2����·
         */
        if(inputWay == 1)System.out.println("��������·����:");
        int linkNum = scanner.nextInt();
        List<Link> links = new ArrayList<>();
        int num1, num2;
        if(inputWay == 1)System.out.println("��������·��Ϣ:(��1 2��ʾ�ڵ�1��2�м�������)");
        for (int i = 1; i <= linkNum; i++) {
            // ��һ���ڵ����
            num1 = scanner.nextInt();
            // �ڶ����ڵ����
            num2 = scanner.nextInt();
            Link link = new Link(i, Constant.bandwidth, nodes.get(num1 - 1), nodes.get(num2 - 1), Constant.delay);
            links.add(link);
            nodes.get(num1 - 1).getPorts().add(link);
            nodes.get(num2 - 1).getPorts().add(link);
        }
        Topo topo = new Topo(nodes, links);
        storeTopo(topo);
        System.out.println("��������ɹ�!");
        return topo;
    }
    public static List<SFC> inputRequest() throws InterruptedException {
        System.out.println("������SFC����");
        Thread.sleep(500);
        System.out.println("��������������:");
        Scanner scanner = new Scanner(System.in);
        int sfcNum = scanner.nextInt();
        printSFCRequestInfo();

        /**
         * ������ʽ����һ��sfc����
         */
        List<SFC> result = new ArrayList<>();
        for (int i = 0; i < sfcNum; i++) {
            result.add(inputSFC(i+1));
        }
        storeSFC(result);
        return result;
    }

    public static void printSFCRequestInfo() {
        System.out.println("������sfc����:\n����:3 1 2 3 1 10 100\n��ʾ��Ҫ����3��vnf,����1, 2, 3, �����Ϊ�ڵ�1��������10, ���������ʱ150");
        System.out.println("���Ͷ�Ӧ��ϵ:");
        List<String> vnfType = Constant.vnfType;
        for (int i = 0; i < vnfType.size(); i++) {
            System.out.println((i+1)+" "+vnfType.get(i));
        }
    }
    public static SFC inputSFC(int index){
        int vnfNum, type, flowRate, delay, accessNodeId;
        List<String> vnfString;
        List<String> vnfType = Constant.vnfType;
        Scanner scanner = new Scanner(System.in);
        vnfNum = scanner.nextInt();
        vnfString = new ArrayList<>();
        while (vnfNum-->0){
            type = scanner.nextInt();
            vnfString.add(vnfType.get(type - 1));
        }
        accessNodeId = scanner.nextInt();
        flowRate = scanner.nextInt();
        delay = scanner.nextInt();
        return new SFC(index, delay, flowRate, accessNodeId, vnfString);
    }


    public static void changeState(Topo topo, int index, boolean isDeployed){
        System.out.println("����������Ҫ�޸ĵ�SFC��������������,��:3 1 2 3");
        Scanner scanner = new Scanner(System.in);
        List<SFC> sfcs = topo.getSfcs();
        List<SFC> sfcList = new ArrayList<>();
        int num = scanner.nextInt();
        for (int i = 0; i < num; i++) {
            int sfcId = scanner.nextInt();
            if(sfcId >= index) {
                System.out.println("�����ڸ�SFC");
                return;
            }
            sfcList.add(sfcs.stream().filter(sfc1 -> sfc1.getSfcId() == sfcId).toList().get(0));
        }
        System.out.println("��������������:");
        int flowRate = scanner.nextInt();
        for(SFC sfc: sfcList){
            if(isDeployed){
                sfc.update(flowRate);
            }
            sfc.setFlowRate(flowRate);
        }

//        storeSFC(sfcs);
    }
    public static void storeSFC(List<SFC> sfcs){
        List<List<VNF>> vnfList = new ArrayList<>();
        List<List<Link>> linkList = new ArrayList<>();
        List<List<VirtualLink>> virtualLinkList = new ArrayList<>();
        sfcs.forEach(sfc -> {
            vnfList.add(sfc.getVnfList());
            linkList.add(sfc.getAccessToVNF0());
            virtualLinkList.add(sfc.getLinkList());
            sfc.setVnfList(null);
            sfc.setAccessToVNF0(null);
            sfc.setLinkList(null);
        });
        String jsonString = JSONUtil.toJsonPrettyStr(sfcs);
        FileUtil.writeString(jsonString, new File("requests.json"), "GBK");
        for (int i = 0; i < sfcs.size(); i++) {
            sfcs.get(i).setVnfList(vnfList.get(i));
            sfcs.get(i).setAccessToVNF0(linkList.get(i));
            sfcs.get(i).setLinkList(virtualLinkList.get(i));
        }
    }
    public static void storeSFC(List<SFC> sfcs, List<SFC> sfcleft){
        List<SFC> sfcList = new ArrayList<>(sfcs);
        sfcList.addAll(sfcleft);
        storeSFC(sfcList);
    }

    public static void storeTopo(Topo topo){
        List<Node> nodes = topo.getNodes();
        List<List<Link>> links = new ArrayList<>();
        nodes.forEach(node -> {
            links.add(node.getPorts());
            node.setPorts(null);
        });
        String jsonString = JSONUtil.toJsonPrettyStr(topo);
        FileUtil.writeString(jsonString, new File("topology.json"), "GBK");
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).setPorts(links.get(i));
        }
    }

    public static Topo readTopoStorage(){
        File file = new File("topology.json");
        Topo topo = null;
        if(file.exists()){
            String jsonString = FileUtil.readString(new File("topology.json"), Charset.forName("GBK"));
            topo = JSONUtil.toBean(jsonString, Topo.class);
        }
        if(topo != null){
            List<Node> nodes = topo.getNodes();
            nodes.forEach(node -> {
                if(node.getPorts() == null) node.setPorts(new ArrayList<>());
            });
            Node fakeNode1, fakeNode2;
            for (Link link: topo.getLinks()) {
                fakeNode1 = link.getNode1();
                fakeNode2 = link.getNode2();
                link.setNode1(nodes.get(fakeNode1.getNodeId() - 1));
                link.setNode2(nodes.get(fakeNode2.getNodeId() - 1));
                link.getNode1().addLink(link);
                link.getNode2().addLink(link);
            }
        }
        return topo;
    }
    public static void storeVNFs(List<VNF> vnfs, List<SFC> sfcs,Topo topo){
        List<List<VNF>> vnfList = new ArrayList<>();
        List<List<Link>> linkList = new ArrayList<>();
        List<List<VirtualLink>> virtualLinkList = new ArrayList<>();
        List<Node> nodes = topo.getNodes();
        List<List<Link>> links = new ArrayList<>();
        List<List<VNF>> vnfList2 = new ArrayList<>();
        nodes.forEach(node -> {
            links.add(node.getPorts());
            vnfList2.add(node.getVnfList());
            node.setPorts(null);
            node.setVnfList(null);
        });
        sfcs.forEach(sfc -> {
            vnfList.add(sfc.getVnfList());
            linkList.add(sfc.getAccessToVNF0());
            virtualLinkList.add(sfc.getLinkList());
            sfc.setVnfList(null);
            sfc.setAccessToVNF0(null);
            sfc.setLinkList(null);
        });
        String jsonString = JSONUtil.toJsonPrettyStr(vnfs);
        FileUtil.writeString(jsonString, new File("result.json"), "GBK");
        for (int i = 0; i < sfcs.size(); i++) {
            sfcs.get(i).setVnfList(vnfList.get(i));
            sfcs.get(i).setAccessToVNF0(linkList.get(i));
            sfcs.get(i).setLinkList(virtualLinkList.get(i));
        }
        for (int i = 0; i < nodes.size(); i++) {
            nodes.get(i).setPorts(links.get(i));
            nodes.get(i).setVnfList(vnfList2.get(i));
        }
    }
    public static List<VNF> readVNFs(List<SFC> sfcs, Topo topo){
        File file = new File("result.json");
        List<VNF> vnfList = null;
        if(file.exists()){
            String jsonString = FileUtil.readString(new File("result.json"), Charset.forName("GBK"));
            vnfList = JSONUtil.parseArray(jsonString,true).toList(VNF.class);
        }
        SFC sfc, s;
        if(vnfList != null && sfcs!= null){
            for(VNF vnf : vnfList){
                for(int j = 0; j < vnf.getSfcList().size(); j++){
                    s = vnf.getSfcList().get(j);
                    int finalSfcId = s.getSfcId();
                    sfc = sfcs.stream().filter(sfc1 -> sfc1.getSfcId() == finalSfcId).findAny().get();
                    sfc.setVnfList(new ArrayList<>(s.getRequiredVNF().size()));
                    for (int i = 0; i < s.getRequiredVNF().size(); i++) {
                        if(s.getRequiredVNF().get(i).equals(vnf.getVnfType())){
                            sfc.setVNF(vnf, i);
                            break;
                        }
                    }
                    vnf.getSfcList().set(j, sfc);
                }
            }
            vnfList.forEach(vnf -> {
                Node node = topo.getNodes().get(vnf.getNode().getNodeId() - 1);
                node.addVNF(vnf);
                vnf.setNode(node);
            });
            sfcs.forEach(sfc1 -> sfc1.updateLinks(topo));
        }
        return vnfList;
    }
    public static List<VNF> inputVNF(Topo topo){
        List<Node> nodes = topo.getNodes();
        List<SFC> sfcs = topo.getSfcs();
        Scanner scanner = new Scanner(System.in);
        System.out.println("������ÿ��SFC�Ĳ���ʽ");
        int nodeNum;
        List<Node> deployResult = new ArrayList<>();
        List<VNF> result = new ArrayList<>();
        for (SFC sfc: sfcs){
            System.out.println(networkInspector.getSFCInfo(sfc));
            for (int i = 0; i < sfc.getRequiredVNF().size(); i++) {
                nodeNum = scanner.nextInt();
                deployResult.add(nodes.get(nodeNum - 1));
            }
            result.addAll(networkInspector.deploySFC(deployResult, sfc));
            deployResult.clear();
        }
        return result;
    }
}
