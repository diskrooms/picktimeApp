package tech.startech.picktime;
import java.util.List;

/**
 * Created by jsb-hdp-0 on 2017/8/14.
 */

public class ModelData {

    private Model data;

    public Model getData() {
        return data;
    }

    public void setData(Model data) {
        this.data = data;
    }

    public static class Model {
        /**
         * title : 眼框酸疼、双眼干涩，谁来救救我……
         * url : http://dxy.com/column/7696
         * cover : http://img.dxycdn.com/dotcom/2016/10/21/21/eobgk3vq.png
         * show_type : 1
         * from : appIndexAdmin
         * author : {"id":735,"name":"晓雅","url":"xiaoya","avatar":"http://img.dxycdn.com/dotcom/2015/09/14/23/9u7rkbqx.png","remarks":"医学科普编辑"}
         */
        private List<ItemsBean> items;

        public List<ItemsBean> getItems() {
            return items;
        }
        public void setItems(List<ItemsBean> items) {
            this.items = items;
        }

        public static class ItemsBean {
            private String title;
            private String url;
            private String cover;
            private String content;
            private int show_type;
            private String from;
            private AuthorBean author;

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getCover() {
                return cover;
            }

            public String getContent(){
                return content;
            }

            public void setCover(String cover) {
                this.cover = cover;
            }

            public int getShow_type() {
                return show_type;
            }

            public void setShow_type(int show_type) {
                this.show_type = show_type;
            }

            public String getFrom() {
                return from;
            }

            public void setFrom(String from) {
                this.from = from;
            }

            public AuthorBean getAuthor() {
                return author;
            }

            public void setAuthor(AuthorBean author) {
                this.author = author;
            }

            public static class AuthorBean {
                private int id;
                private String name;
                private String url;
                private String avatar;
                private String remarks;

                public int getId() {
                    return id;
                }

                public void setId(int id) {
                    this.id = id;
                }

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }

                public String getUrl() {
                    return url;
                }

                public void setUrl(String url) {
                    this.url = url;
                }

                public String getAvatar() {
                    return avatar;
                }

                public void setAvatar(String avatar) {
                    this.avatar = avatar;
                }

                public String getRemarks() {
                    return remarks;
                }

                public void setRemarks(String remarks) {
                    this.remarks = remarks;
                }
            }
        }
    }
}
