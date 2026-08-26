package com.joyhsin.hotspot;
import java.time.Instant; import java.util.*; import java.util.stream.*;
import org.springframework.stereotype.Service; import org.springframework.web.multipart.MultipartFile;
@Service
public class SceneService {
  private final SceneStore store; private final ImageStorageService images;
  public SceneService(SceneStore s,ImageStorageService i){store=s;images=i;}
  public List<SceneData> list(){return store.read();} public SceneData get(long id){return store.read().stream().filter(s->s.id()==id).findFirst().orElseThrow(()->new NoSuchElementException("场景不存在"));}
  public SceneData create(String name,MultipartFile image){ if(name==null||name.isBlank()) throw new IllegalArgumentException("场景名称不能为空"); SceneData s=new SceneData(nextId(),name.trim(),images.save(image),List.of(),List.of(),1,0,Instant.now(),null); saveReplacing(s); return s; }
  public SceneData sample(){ var hs=List.of(h("pos",.22,.28,"智慧收银","#2563EB","POS-01","双屏智能 POS","¥2,999","/sample/products/pos.svg","支持收银、会员、营销和订单聚合。"),h("wifi",.67,.20,"全场 Wi-Fi","#7C3AED","NET-06","Wi-Fi 6 企业级接入点","¥1,299","/sample/products/wifi.svg","为餐厅提供稳定的无线网络覆盖。"),h("robot",.52,.58,"送餐机器人","#EA580C","BOT-03","智能送餐机器人","¥19,999","/sample/products/robot.svg","自动规划路线并将餐品送达桌边。"),h("screen",.80,.72,"数字菜单","#059669","DSP-04","商用数字标牌","¥3,699","/sample/products/screen.svg","远程更新菜单与营销内容。")); SceneData s=new SceneData(nextId(),"智慧餐饮门店示例","/sample/restaurant-layout.svg",hs,hs,1,1,Instant.now(),Instant.now()); saveReplacing(s); return s; }
  public SceneData draft(long id,List<SceneData.Hotspot> hs){ validateUrls(hs); hs=hs.stream().map(this::withId).toList(); SceneData old=get(id),n=new SceneData(old.id(),old.name(),old.imageUrl(),hs,old.publishedHotspots(),old.draftVersion()+1,old.publishedVersion(),Instant.now(),old.publishedAt()); saveReplacing(n);return n; }
  public SceneData publish(long id){SceneData old=get(id),n=new SceneData(old.id(),old.name(),old.imageUrl(),old.draftHotspots(),old.draftHotspots(),old.draftVersion(),old.draftVersion(),Instant.now(),Instant.now());saveReplacing(n);return n;}
  public void delete(long id){var all=store.read(); if(all.removeIf(s->s.id()==id)) store.write(all); else throw new NoSuchElementException("场景不存在");}
  private void saveReplacing(SceneData s){var all=store.read();all.removeIf(x->x.id()==s.id());all.add(s);store.write(all);}
  private void validateUrls(List<SceneData.Hotspot> hs){for(var h:hs){String detail=n(h.product().detailUrl());if(!detail.isBlank()&&!detail.startsWith("/")&&!detail.matches("https?://.+")) throw new IllegalArgumentException("Product detail URL must be an http(s) URL or root-relative path");String image=n(h.product().imageUrl());if(!image.isBlank()&&!image.startsWith("/")&&!image.matches("https?://.+")) throw new IllegalArgumentException("Product image URL must be an http(s) URL or root-relative path");}}
  private long nextId(){return store.read().stream().mapToLong(SceneData::id).max().orElse(0)+1;} private SceneData.Hotspot withId(SceneData.Hotspot h){return h.id()==null||h.id().isBlank()?new SceneData.Hotspot(UUID.randomUUID().toString(),h.xRatio(),h.yRatio(),h.label()==null?"":h.label(),h.markerColor(),h.product()):h;}
  private String n(String s){return s==null?"":s;} private SceneData.Hotspot h(String id,double x,double y,String l,String c,String code,String name,String price,String img,String d){return new SceneData.Hotspot(id,x,y,l,c,new SceneData.Product(code,name,price,img,d,"https://example.com/products/"+id));}
}
