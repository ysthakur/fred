#include <stdio.h>
#include <stdlib.h>

typedef enum
{
  kBlack, // green
  kGray,  // red
  kWhite  // blue
} Color;

struct PCR {
  void *obj;
  void (*markGray)(void *);
  void (*scan)(void *);
  void (*collectWhite)(void *);
  struct PCR *next;
};

struct PCR *pcrs;

void addPCR(void *obj, void (*markGray)(void *), void (*scan)(void *), void (*collectWhite)(void *)) {
  struct PCR *pcr = malloc(sizeof(struct PCR));
  pcr->obj = obj;
  pcr->markGray = markGray;
  pcr->scan = scan;
  pcr->collectWhite = collectWhite;
  pcr->next = pcrs;
  pcrs = pcr;
}

void markGrayAllPCRs(struct PCR *head) {
  if (head == NULL) return;
  struct PCR *next = head->next;
  head->markGray(head->obj);
  markGrayAllPCRs(next);
}

void scanAllPCRs(struct PCR *head) {
  if (head == NULL) return;
  struct PCR *next = head->next;
  head->scan(head->obj);
  scanAllPCRs(next);
}

void collectWhiteAllPCRs(struct PCR *head) {
  if (head == NULL) return;
  struct PCR *next = head->next;
  head->collectWhite(head->obj);
  free(head);
  collectWhiteAllPCRs(next);
}

void processAllPCRs(struct PCR *head) {
  markGrayAllPCRs(head);
  scanAllPCRs(head);
  collectWhiteAllPCRs(head);
}

typedef struct Node_s
{
  int rc;
  Color color;
  int value;
  struct Node_s *left;
  struct Node_s *right;
} *Node;

void markGray(Node node)
{
  printf("[markGray]: Node: %d, rc: %d\n", node->value, node->rc);
  if (node->color != kGray)
  {
    node->color = kGray;
    if (node->left)
    {
      node->left->rc--;
      markGray(node->left);
    }
    if (node->right)
    {
      node->right->rc--;
      markGray(node->right);
    }
  }
}

void scanBlack(Node node)
{
  printf("[scanBlack] Node: %d, rc: %d\n", node->value, node->rc);
  if (node->color == kBlack)
  {
    return;
  }
  node->color = kBlack;
  if (node->left)
  {
    node->left->rc++;
    scanBlack(node->left);
  }
  if (node->right)
  {
    node->right->rc++;
    scanBlack(node->right);
  }
}

void scan(Node node)
{
  printf("[scan] Node: %d, rc: %d\n", node->value, node->rc);
  if (node->color != kGray)
  {
    return;
  }
  if (node->rc > 0)
  {
    // This node is reachable from a GC root!
    scanBlack(node);
    return;
  }
  node->color = kWhite;
  if (node->left)
  {
    scan(node->left);
  }
  if (node->right)
  {
    scan(node->right);
  }
}

void collectWhite(Node node)
{
  printf("[collectWhite] Node: %d, rc: %d\n", node->value, node->rc);
  if (node->color == kWhite)
  {
    node->color = kBlack; // Mark it black so that it won't be entered again
    if (node->left)
      collectWhite(node->left);
    if (node->right)
      collectWhite(node->right);
    // If there were other, acyclic, references, they would have their refcounts decremented here
    printf("[collectWhite] Freeing node %d\n", node->value);
    free(node);
  }
}

void decrRc(Node node)
{
  if (node == NULL || node->rc == 0)
    return;
  printf("[decrRc] Node: %d, rc: %d\n", node->value, node->rc);
  node->rc--;
  if (node->rc == 0)
  {
    decrRc(node->left);
    decrRc(node->right);
    free(node);
  }
  else
  {
    markGray(node);
    scan(node);
    collectWhite(node);
  }
}

void incrRc(Node node)
{
  if (node != NULL)
  {
    node->rc++;
  }
}

void setLeft(Node node, Node left)
{
  decrRc(node->left);
  node->left = left;
  incrRc(left);
}

void setRight(Node node, Node right)
{
  decrRc(node->right);
  node->right = right;
  incrRc(right);
}

Node newNode(int value, Node left, Node right)
{
  // Unlike malloc, calloc sets everything to 0. This means that left and right
  // are set to NULL and rc is set to 0
  Node node = malloc(sizeof(struct Node_s));
  node->rc = 0;
  node->color = kBlack;
  node->value = value;
  node->left = NULL;
  setLeft(node, left);
  node->right = NULL;
  setRight(node, right);
  return node;
}

int example1()
{
  Node a = newNode(10, NULL, NULL);
  incrRc(a);
  Node b = newNode(20, a, NULL);
  incrRc(b);
  setRight(a, b); // We now have a cycle

  int res = a->value + b->value;
  decrRc(a); // The variable a goes out of scope
  decrRc(b);
  return res;
}

int main()
{
  printf("Result: %d\n", example1());
}
