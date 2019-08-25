package d.d.timer;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import java.io.Serializable;

class Timer implements Serializable {
    private long endTime = -1, duration;
    private String id;

    private static final long serialVersionUID = 0;

    public Timer(String id, long duration) {
        this.id = id;
        this.duration = duration;
    }

    public Long calculateEndTime(){
        return (this.endTime = System.currentTimeMillis() + this.duration);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getNotificationId(){
        return id.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(!(obj instanceof Timer)) return false;

        return ((Timer) obj).getId().equals(getId());
    }
}

