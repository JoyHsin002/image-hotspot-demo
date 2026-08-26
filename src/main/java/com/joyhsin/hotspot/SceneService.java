package com.joyhsin.hotspot;
import java.time.Instant; import java.util.*; import java.util.stream.*;
import org.springframework.stereotype.Service; import org.springframework.web.multipart.MultipartFile;
@Service
public class SceneService {
  private final SceneStore store; private final ImageStorageService images;
  public SceneService(SceneStore s,ImageStorageService i){store=s;images=i;}
  public List<SceneData> list(){return store.read();} public SceneData get(String id){return store.read().stream().filter(s->s.id().equals(id)).findFirst().orElseThrow(()->new NoSuchElementException("场景不存在"));}
  public SceneData create(String name,MultipartFile image){ if(name==null||name.isBlank()) throw new IllegalArgumentException("场景名称不能为空"); SceneData s=new SceneData(UUID.randomUUID().toString(),name.trim(),images.save(image),List.of(),List.of(),Instant.now(),null); saveReplacing(s); return s; }
  public SceneData sample(){ var hs=List.of(h("pos",.22,.28,"智慧收银","#2563EB","POS-01","双屏智能 POS","¥2,999","/sample/products/pos.svg","支持收银、会员、营销和订单聚合。"),h("wifi",.67,.20,"全场 Wi-Fi","#7C3AED","NET-02","企业级 Wi-Fi 6","¥1,299","/sample/products/wifi.svg","为餐厅提供稳定的无线网络覆盖。"),h("robot",.52,.58,"送餐机器人","#EA580C","BOT-03","智能送餐机器人","¥19,999","/sample/products/robot.svg","自动规划路线并将餐品送达桌边。"),h("screen",.80,.72,"数字菜单","#059669","DSP-04","商用数字标牌","¥3,699","/sample/products/screen.svg","远程更新菜单与营销内容。")); SceneData s=new SceneData(UUID.randomUUID().toString(),"智慧餐厅示例","/sample/restaurant-layout.svg",hs,hs,Instant.now(),Instant.now()); saveReplacing(s); return s; }
  public SceneData draft(String id,List<SceneData.Hotspot> hs){ validateUrls(hs); SceneData old=get(id),n=new SceneData(old.id(),old.name(),old.imageUrl(),hs,old.publishedHotspots(),Instant.now(),old.publishedAt()); saveReplacing(n);return n; }
  public SceneData publish(String id){SceneData old=get(id),n=new SceneData(old.id(),old.name(),old.imageUrl(),old.draftHotspots(),old.draftHotspots(),Instant.now(),Instant.now());saveReplacing(n);return n;}
  public void delete(String id){var all=store.read(); if(all.removeIf(s->s.id().equals(id))) store.write(all); else throw new NoSuchElementException("场景不存在");}
  private void saveReplacing(SceneData s){var all=store.read();all.removeIf(x->x.id().equals(s.id()));all.add(s);store.write(all);}
  private void validateUrls(List<SceneData.Hotspot> hs){for(var h:hs) for(String u:List.of(n(h.product().imageUrl()),n(h.product().detailUrl()))) if(!u.isBlank()&&!u.startsWith("/")&&!u.matches("https?://.+")) throw new IllegalArgumentException("商品 URL 仅允许 http、https 或站内根路径");}
  private String n(String s){return s==null?"":s;} private SceneData.Hotspot h(String id,double x,double y,String l,String c,String code,String name,String price,String img,String d){return new SceneData.Hotspot(id,x,y,l,c,new SceneData.Product(code,name,price,img,d,"https://example.com/products/"+id));}
}
