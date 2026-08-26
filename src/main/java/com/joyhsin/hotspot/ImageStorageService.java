package com.joyhsin.hotspot;
import java.io.*; import java.nio.file.*; import java.util.*;
import org.springframework.core.io.Resource; import org.springframework.core.io.UrlResource; import org.springframework.stereotype.Service; import org.springframework.web.multipart.MultipartFile;
@Service
public class ImageStorageService {
  private static final Map<String,String> TYPES=Map.of("png","image/png","jpg","image/jpeg","gif","image/gif","webp","image/webp"); private final Path root;
  public ImageStorageService(AppProperties props) throws IOException { root=props.dataDirectory().toAbsolutePath().normalize().resolve("uploads"); Files.createDirectories(root); }
  public String save(MultipartFile file){ try { if(file.isEmpty()||file.getSize()>10*1024*1024) throw new IllegalArgumentException("图片为空或超过 10 MB"); byte[] b=file.getBytes(); String ext=detect(b); if(ext==null) throw new IllegalArgumentException("仅支持 PNG、JPEG、GIF、WebP 图片"); String name=UUID.randomUUID()+"."+ext; Files.write(root.resolve(name),b,StandardOpenOption.CREATE_NEW); return "/uploads/"+name; } catch(IOException e){ throw new IllegalStateException("图片保存失败",e); } }
  public Resource load(String name){ try { if(!name.matches("[a-f0-9-]+\\.(png|jpg|gif|webp)")) throw new IllegalArgumentException("非法图片路径"); Path p=root.resolve(name).normalize(); if(!p.getParent().equals(root)) throw new IllegalArgumentException("非法图片路径"); Resource r=new UrlResource(p.toUri()); if(!r.exists()) throw new NoSuchElementException("图片不存在"); return r; } catch(IOException e){ throw new IllegalStateException(e); } }
  private String detect(byte[] b){ if(b.length>=8&&b[0]==(byte)0x89&&b[1]==0x50&&b[2]==0x4e&&b[3]==0x47) return "png"; if(b.length>=3&&b[0]==(byte)0xff&&b[1]==(byte)0xd8&&b[2]==(byte)0xff) return "jpg"; if(b.length>=6&&new String(b,0,6).startsWith("GIF8")) return "gif"; if(b.length>=12&&new String(b,0,4).equals("RIFF")&&new String(b,8,4).equals("WEBP")) return "webp"; return null; }
}
