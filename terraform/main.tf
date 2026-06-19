terraform {
  required_version = ">= 1.6"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Upgrade to S3 backend before applying to production:
  # backend "s3" {
  #   bucket = "your-tfstate-bucket"
  #   key    = "job-scanner/terraform.tfstate"
  #   region = "ap-southeast-2"
  # }
}

provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Project     = "job-scanner"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

module "vpc" {
  source       = "./modules/vpc"
  cluster_name = var.cluster_name
  environment  = var.environment
}

module "security" {
  source       = "./modules/security"
  cluster_name = var.cluster_name
  vpc_id       = module.vpc.vpc_id
  vpc_cidr     = module.vpc.vpc_cidr
}

module "eks" {
  source              = "./modules/eks"
  cluster_name        = var.cluster_name
  environment         = var.environment
  vpc_id              = module.vpc.vpc_id
  private_subnet_ids  = module.vpc.private_subnet_ids
  public_subnet_ids   = module.vpc.public_subnet_ids
  node_instance_type  = var.node_instance_type
  node_count          = var.node_count
  eks_node_sg_id      = module.security.eks_node_sg_id
}

module "rds" {
  source            = "./modules/rds"
  cluster_name      = var.cluster_name
  environment       = var.environment
  vpc_id            = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  rds_sg_id         = module.security.rds_sg_id
  db_username       = var.db_username
  db_password       = var.db_password
}

module "elasticache" {
  source             = "./modules/elasticache"
  cluster_name       = var.cluster_name
  environment        = var.environment
  private_subnet_ids = module.vpc.private_subnet_ids
  redis_sg_id        = module.security.redis_sg_id
}
