#include "runtime.h"

enum Player_kind { Player_tag };
struct Player {
  int rc;
  enum Color color;
  int addedPCR;
  enum Player_kind kind;
  void (*print)();
  struct PlayerList* friends;
  struct Store* store;
  union {
    struct {  };
  };
};
enum PlayerList_kind { PlayerNil_tag, PlayerCons_tag };
struct PlayerList {
  int rc;
  enum Color color;
  int addedPCR;
  enum PlayerList_kind kind;
  void (*print)();
  union {
    struct {  };
    struct { struct Player* player_PlayerCons; struct PlayerList* next_PlayerCons; };
  };
};
enum Store_kind { Store_tag };
struct Store {
  int rc;
  enum Color color;
  int addedPCR;
  enum Store_kind kind;
  void (*print)();
  struct Data* datums;
  union {
    struct {  };
  };
};
enum Data_kind { DataCons_tag, DataNil_tag };
struct Data {
  int rc;
  enum Color color;
  int addedPCR;
  enum Data_kind kind;
  void (*print)();
  union {
    struct { int value_DataCons; struct Data* next_DataCons; };
    struct {  };
  };
};
void $free_Player(struct Player* this);
void $free_PlayerList(struct PlayerList* this);
void $free_Store(struct Store* this);
void $free_Data(struct Data* this);
void $decr_Player(struct Player* this);
void $decr_PlayerList(struct PlayerList* this);
void $decr_Store(struct Store* this);
void $decr_Data(struct Data* this);
void $markGray_Player(struct Player* this);
void $markGray_PlayerList(struct PlayerList* this);
void $markGray_Store(struct Store* this);
void $markGray_Data(struct Data* this);
void $scan_Player(struct Player* this);
void $scan_PlayerList(struct PlayerList* this);
void $scan_Store(struct Store* this);
void $scan_Data(struct Data* this);
void $scanBlack_Player(struct Player* this);
void $scanBlack_PlayerList(struct PlayerList* this);
void $scanBlack_Store(struct Store* this);
void $scanBlack_Data(struct Data* this);
void $collectWhite_Player(struct Player* this);
void $collectWhite_PlayerList(struct PlayerList* this);
void $collectWhite_Store(struct Store* this);
void $collectWhite_Data(struct Data* this);
void $print_Player(struct Player* this);
void $print_PlayerList(struct PlayerList* this);
void $print_Store(struct Store* this);
void $print_Data(struct Data* this);
struct Player* new$Player(struct PlayerList* friends, struct Store* store);
struct PlayerList* new$PlayerNil();
struct PlayerList* new$PlayerCons(struct PlayerList* next, struct Player* player);
struct Store* new$Store(struct Data* datums);
struct Data* new$DataCons(struct Data* next, int value);
struct Data* new$DataNil();
struct Data* fn$createDummyData(int length);
int main();
void $free_Player(struct Player* this) {
  switch (this->kind) {
  case Player_tag:
    $decr_Store(this->store);
    break;
  }
  free(this);
}
void $free_PlayerList(struct PlayerList* this) {
  switch (this->kind) {
  case PlayerNil_tag:
    break;
  case PlayerCons_tag:
    break;
  }
  free(this);
}
void $free_Store(struct Store* this) {
  switch (this->kind) {
  case Store_tag:
    $decr_Data(this->datums);
    break;
  }
  free(this);
}
void $free_Data(struct Data* this) {
  switch (this->kind) {
  case DataCons_tag:
    break;
  case DataNil_tag:
    break;
  }
  free(this);
}
void $decr_Player(struct Player* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Player_tag:
      $decr_PlayerList(this->friends);
      $decr_Store(this->store);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_Player,
      (void *) $scan_Player,
      (void *) $collectWhite_Player);
  }
}
void $decr_PlayerList(struct PlayerList* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case PlayerNil_tag:
      break;
    case PlayerCons_tag:
      $decr_Player(this->player_PlayerCons);
      $decr_PlayerList(this->next_PlayerCons);
      break;
    }
    removePCR((void *) this, 0);
    free(this);
  } else {
    addPCR(
      (void *) this,
      0,
      (void *) $markGray_PlayerList,
      (void *) $scan_PlayerList,
      (void *) $collectWhite_PlayerList);
  }
}
void $decr_Store(struct Store* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case Store_tag:
      $decr_Data(this->datums);
      break;
    }
    removePCR((void *) this, 1);
    free(this);
  }
}
void $decr_Data(struct Data* this) {
  if (--this->rc == 0) {
    switch (this->kind) {
    case DataCons_tag:
      $decr_Data(this->next_DataCons);
      break;
    case DataNil_tag:
      break;
    }
    removePCR((void *) this, 2);
    free(this);
  } else {
    addPCR(
      (void *) this,
      2,
      (void *) $markGray_Data,
      (void *) $scan_Data,
      (void *) $collectWhite_Data);
  }
}
void $markGray_Player(struct Player* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Player_tag:
    this->friends->rc --;
    $markGray_PlayerList(this->friends);
    break;
  }
}
void $markGray_PlayerList(struct PlayerList* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case PlayerNil_tag:
    break;
  case PlayerCons_tag:
    this->player_PlayerCons->rc --;
    $markGray_Player(this->player_PlayerCons);
    this->next_PlayerCons->rc --;
    $markGray_PlayerList(this->next_PlayerCons);
    break;
  }
}
void $markGray_Store(struct Store* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case Store_tag:
    break;
  }
}
void $markGray_Data(struct Data* this) {
  if (this->color == kGray) return;
  this->color = kGray;
  this->addedPCR = 0;
  switch (this->kind) {
  case DataCons_tag:
    this->next_DataCons->rc --;
    $markGray_Data(this->next_DataCons);
    break;
  case DataNil_tag:
    break;
  }
}
void $scan_Player(struct Player* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Player(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Player_tag:
    $scan_PlayerList(this->friends);
    break;
  }
}
void $scan_PlayerList(struct PlayerList* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_PlayerList(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case PlayerNil_tag:
    break;
  case PlayerCons_tag:
    $scan_Player(this->player_PlayerCons);
    $scan_PlayerList(this->next_PlayerCons);
    break;
  }
}
void $scan_Store(struct Store* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Store(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case Store_tag:
    break;
  }
}
void $scan_Data(struct Data* this) {
  if (this->color != kGray) return;
  if (this->rc > 0) {
    $scanBlack_Data(this);
    return;
  }
  this->color = kWhite;
  switch (this->kind) {
  case DataCons_tag:
    $scan_Data(this->next_DataCons);
    break;
  case DataNil_tag:
    break;
  }
}
void $scanBlack_Player(struct Player* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Player_tag:
      this->friends->rc ++;
      $scanBlack_PlayerList(this->friends);
      break;
    }
  }
}
void $scanBlack_PlayerList(struct PlayerList* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case PlayerNil_tag:
      break;
    case PlayerCons_tag:
      this->player_PlayerCons->rc ++;
      $scanBlack_Player(this->player_PlayerCons);
      this->next_PlayerCons->rc ++;
      $scanBlack_PlayerList(this->next_PlayerCons);
      break;
    }
  }
}
void $scanBlack_Store(struct Store* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case Store_tag:
      break;
    }
  }
}
void $scanBlack_Data(struct Data* this) {
  if (this->color != kBlack) {
    this->color = kBlack;
    switch (this->kind) {
    case DataCons_tag:
      this->next_DataCons->rc ++;
      $scanBlack_Data(this->next_DataCons);
      break;
    case DataNil_tag:
      break;
    }
  }
}
void $collectWhite_Player(struct Player* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Player_tag:
      $collectWhite_PlayerList(this->friends);
      $collectWhite_Store(this->store);
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Player;
  }
}
void $collectWhite_PlayerList(struct PlayerList* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case PlayerNil_tag:
      break;
    case PlayerCons_tag:
      $collectWhite_Player(this->player_PlayerCons);
      $collectWhite_PlayerList(this->next_PlayerCons);
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_PlayerList;
  }
}
void $collectWhite_Store(struct Store* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case Store_tag:
      $collectWhite_Data(this->datums);
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Store;
  }
}
void $collectWhite_Data(struct Data* this) {
  if (this->color == kWhite) {
    this->color = kBlack;
    switch (this->kind) {
    case DataCons_tag:
      $collectWhite_Data(this->next_DataCons);
      break;
    case DataNil_tag:
      break;
    }
    struct FreeCell *curr = freeList;
    freeList = malloc(sizeof(struct FreeCell));
    freeList->obj = (void *) this;
    freeList->next = curr;
    freeList->free = (void *) $free_Data;
  }
}
void $print_Player(struct Player* this) {
  switch (this->kind) {
  case Player_tag:
    printf("Player {");
    printf("friends=");
    $print_PlayerList(this->friends);
    printf(", ");
    printf("store=");
    $print_Store(this->store);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_PlayerList(struct PlayerList* this) {
  switch (this->kind) {
  case PlayerNil_tag:
    printf("PlayerNil {");
    printf("}");
    break;
  case PlayerCons_tag:
    printf("PlayerCons {");
    printf("player=");
    $print_Player(this->player_PlayerCons);
    printf(", ");
    printf("next=");
    $print_PlayerList(this->next_PlayerCons);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_Store(struct Store* this) {
  switch (this->kind) {
  case Store_tag:
    printf("Store {");
    printf("datums=");
    $print_Data(this->datums);
    printf(", ");
    printf("}");
    break;
  }
}
void $print_Data(struct Data* this) {
  switch (this->kind) {
  case DataCons_tag:
    printf("DataCons {");
    printf("value=");
    printf("%d", this->value_DataCons);
    printf(", ");
    printf("next=");
    $print_Data(this->next_DataCons);
    printf(", ");
    printf("}");
    break;
  case DataNil_tag:
    printf("DataNil {");
    printf("}");
    break;
  }
}
struct Player* new$Player(struct PlayerList* friends, struct Store* store) {
  struct Player* $res = malloc(sizeof (struct Player));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Player;
  $res->kind = Player_tag;
  $res->friends = friends;
  $res->friends->rc ++;
  $res->store = store;
  $res->store->rc ++;
  return $res;
}
struct PlayerList* new$PlayerNil() {
  struct PlayerList* $res = malloc(sizeof (struct PlayerList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_PlayerList;
  $res->kind = PlayerNil_tag;
  return $res;
}
struct PlayerList* new$PlayerCons(struct PlayerList* next, struct Player* player) {
  struct PlayerList* $res = malloc(sizeof (struct PlayerList));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_PlayerList;
  $res->kind = PlayerCons_tag;
  $res->player_PlayerCons = player;
  $res->player_PlayerCons->rc ++;
  $res->next_PlayerCons = next;
  $res->next_PlayerCons->rc ++;
  return $res;
}
struct Store* new$Store(struct Data* datums) {
  struct Store* $res = malloc(sizeof (struct Store));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Store;
  $res->kind = Store_tag;
  $res->datums = datums;
  $res->datums->rc ++;
  return $res;
}
struct Data* new$DataCons(struct Data* next, int value) {
  struct Data* $res = malloc(sizeof (struct Data));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Data;
  $res->kind = DataCons_tag;
  $res->value_DataCons = value;
  $res->next_DataCons = next;
  $res->next_DataCons->rc ++;
  return $res;
}
struct Data* new$DataNil() {
  struct Data* $res = malloc(sizeof (struct Data));
  $res->rc = 0;
  $res->color = kBlack;
  $res->addedPCR = 0;
  $res->print = $print_Data;
  $res->kind = DataNil_tag;
  return $res;
}
struct Data* fn$createDummyData(int length) {
  struct Data* ifres$0;
  if (length == 0) {
    ifres$0 = new$DataNil();
  } else {
    ifres$0 = new$DataCons(fn$createDummyData(length - 1), length);
  }
  struct Data* ret$1 = ifres$0;
  return ret$1;
}
int main() {
  long start = __rdtscp(NULL);
  struct Data* dummyData = fn$createDummyData(500);
  dummyData->rc ++;
  __rdtscp(NULL) - start
  printf("%d", 0);
  int ret$2 = 0;
  $decr_Data(dummyData);
  processAllPCRs();
  return ret$2;
}
