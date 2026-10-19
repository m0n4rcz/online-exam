CC ?= gcc
CFLAGS = -O2 -Wall -Ilib -I.
LDFLAGS = -lpthread -ldl -lm

ifeq ($(OS),Windows_NT)
    LDFLAGS += -lws2_32
endif

SRCS = server.c codec.c db.c session.c room.c dispatcher.c sha256.c lib/cJSON.c lib/sqlite3.c
OBJS = $(SRCS:.c=.o)
TARGET = server

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(OBJS) -o $@ $(LDFLAGS)

%.o: %.c
	$(CC) $(CFLAGS) -c $< -o $@

clean:
	rm -f $(OBJS) $(TARGET) $(TARGET).exe
