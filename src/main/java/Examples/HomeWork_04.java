package Examples;

import javax.print.attribute.IntegerSyntax;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HomeWork_04 {

    public static void main(String[] args) throws IOException {

/*        Path dir = Paths.get("dir");

        List<String> files = Files.list(dir)
                .filter(path -> !Files.isDirectory(path))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toList());
        System.out.println(files);

        String filesStr = Files.list(dir)
                .filter(path -> !Files.isDirectory(path))
                .map(path -> path.getFileName().toString())
                .collect(Collectors.joining(", "));

        System.out.println(filesStr);
*/
        Set<Integer> shopsA = new HashSet<>();
        shopsA.add(1);
        shopsA.add(2);
        shopsA.add(3);
        Set<Integer> shopsB = new HashSet<>();
        shopsB.add(4);
        shopsB.add(5);
        shopsB.add(6);
        Set<Integer> shopsC = new HashSet<>();
        shopsC.add(7);
        shopsC.add(8);
        shopsC.add(9);

        Set<Integer> business = new HashSet<>();
        business.add(8080);   //бытовая техника
        business.add(9090);   //телефоны
        business.add(3030);   //продукты
        business.add(4004);   //детский магазин

        Map<Integer, Set<Integer>> vendorsShops = new HashMap<>();
        vendorsShops.put(510, shopsA);
        vendorsShops.put(530, shopsB);
        vendorsShops.put(560, shopsC);

        Map<Integer, Integer> shopsBusiness = new HashMap<>();
        shopsBusiness.put(1, 3030);
        shopsBusiness.put(2, 9090);
        shopsBusiness.put(3, 7090);
        shopsBusiness.put(4, 8080);
        shopsBusiness.put(5, 3030);
        shopsBusiness.put(6, 4004);
        shopsBusiness.put(7, 5504);
        shopsBusiness.put(8, 7090);
        shopsBusiness.put(9, 8080);

        //System.out.println(getBusinessMap(vendorsShops, shopsBusiness));
        getBusinessMap(vendorsShops, shopsBusiness);

    }

    /**
     * Сделать через стримы
     *
     * @param shopsBusiness - бизнесы магазинов
     * @param vendorsShops  - магазины вендора
     * @return бизнесы вендора
     */
    static Map<Integer, Set<Integer>> getBusinessMap(
            Map<Integer, Set<Integer>> vendorsShops,
            Map<Integer, Integer> shopsBusiness
    ) {
        //Решение без использования стримов
/*        Map<Integer, Set<Integer>> businessMap = new HashMap<>();
        Iterator<Map.Entry<Integer, Set<Integer>>> vendorIterator = vendorsShops.entrySet().iterator();
        while (vendorIterator.hasNext()) {
            Map.Entry<Integer, Set<Integer>> pair = vendorIterator.next();
            Integer vendor = pair.getKey();
            Set<Integer> shops = pair.getValue();
            businessMap.put(vendor, new HashSet<>());
            for (Integer s : shops) {
                Iterator<Map.Entry<Integer, Integer>> businessIterator = shopsBusiness.entrySet().iterator();
                while (businessIterator.hasNext()) {
                    Map.Entry<Integer, Integer> pair2 = businessIterator.next();
                    Integer shop = pair2.getKey();
                    Integer business = pair2.getValue();
                    if (s == shop) {
                        businessMap.get(vendor).add(business);
                    }
                }
            }
        }
*/
        //Решение на стримах2
        Map<Integer, Set<Integer>> businessMap = new HashMap<>();
        vendorsShops.entrySet().stream()
                .forEach(pair -> {
                    HashSet<Integer> setBus = new HashSet();
                    shopsBusiness.keySet().stream()
                            .forEach(setBusShops -> {
                                pair.getValue().stream()
                                        .forEach(vShop -> {
                                            if (setBusShops == vShop) {
                                                setBus.add(shopsBusiness.get(setBusShops));
                                                //System.out.println("vShop #" + vShop + " = " + shopsBusiness.get(setBusShops));
                                            }
                                        });
                            });
                    //System.out.println(pair.getKey() + " = " + setBus);
                    businessMap.put(pair.getKey(), setBus);
                });
        return businessMap;
    }
}