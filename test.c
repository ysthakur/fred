#include <stdio.h>
#include <stdlib.h>

typedef struct Node_s
{
  int rc;
  int value;
  struct Node_s *left;
  struct Node_s *right;
} *Node;

void decrRc(Node node)
{
  if (node == NULL || node->rc == 0)
    return;
  node->rc--;
  if (node->rc == 0)
  {
    decrRc(node->left);
    decrRc(node->right);
    free(node);
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
  // setRight(a, b); // We now have a cycle

  decrRc(a); // The variable a goes out of scope
  decrRc(b);
}

int main()
{
  example1();
}
